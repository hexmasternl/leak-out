#!/usr/bin/env node
// Keeps Claude Code and GitHub Copilot configuration in sync.
//
// Counterpart groups (a change to any member is copied to the others):
//   CLAUDE.md  <->  AGENTS.md  <->  .github/copilot-instructions.md   (identical copies)
//   .claude/skills/<skill>/**  <->  .github/skills/<skill>/**          (identical copies)
//   .claude/commands/<dir>/<x>.md  <->  .github/prompts/<dir>-<x>.prompt.md
//   .claude/agents/<x>.md          <->  .github/agents/<x>.agent.md
// Commands/prompts and agents share their body and a few frontmatter keys; keys that only
// one tool understands (tools, model, category, tags, ...) are left alone on each side.
//
// Which side changed is decided against a baseline: the content recorded at the last sync
// (stored per clone/worktree in .git/agent-sync-state.json), falling back to HEAD.
//
// Usage:
//   node scripts/sync-agent-config.mjs                 sync everything
//   node scripts/sync-agent-config.mjs --from <path>   treat <path> as the source of truth for its group
//   node scripts/sync-agent-config.mjs --prefer-newest resolve conflicts by newest modification time
//   node scripts/sync-agent-config.mjs --check         report drift, change nothing, exit 1 if out of sync
//   node scripts/sync-agent-config.mjs --hook          run as a Claude Code / Copilot hook (JSON on stdin)

import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const INSTRUCTION_FILES = ['CLAUDE.md', 'AGENTS.md', '.github/copilot-instructions.md'];
const COMMAND_SHARED_KEYS = ['description', 'argument-hint'];
const AGENT_SHARED_KEYS = ['name', 'description'];

// ---------------------------------------------------------------------------------------
// File helpers

const toPosix = (p) => p.split(path.sep).join('/');
const abs = (rel) => path.join(ROOT, rel);
const normalize = (text) => text.replace(/^﻿/, '').replace(/\r\n/g, '\n');
const hash = (text) => (text === null ? null : createHash('sha256').update(normalize(text)).digest('hex'));

function readFile(rel) {
  try {
    return fs.readFileSync(abs(rel), 'utf8');
  } catch {
    return null;
  }
}

function mtime(rel) {
  try {
    return fs.statSync(abs(rel)).mtimeMs;
  } catch {
    return 0;
  }
}

function walk(relDir, filter = () => true) {
  const out = [];
  const visit = (rel) => {
    let entries;
    try {
      entries = fs.readdirSync(abs(rel), { withFileTypes: true });
    } catch {
      return;
    }
    for (const entry of entries) {
      const child = `${rel}/${entry.name}`;
      if (entry.isDirectory()) visit(child);
      else if (entry.isFile() && filter(child)) out.push(child);
    }
  };
  visit(relDir);
  return out;
}

function writeFile(rel, text) {
  fs.mkdirSync(path.dirname(abs(rel)), { recursive: true });
  fs.writeFileSync(abs(rel), text, 'utf8');
}

function removeFile(rel) {
  fs.rmSync(abs(rel), { force: true });
  // Remove directories left empty, but never the tool roots themselves.
  let dir = path.posix.dirname(rel);
  while (!['.', '.claude', '.github', '.claude/skills', '.github/skills'].includes(dir)) {
    try {
      fs.rmdirSync(abs(dir));
    } catch {
      break;
    }
    dir = path.posix.dirname(dir);
  }
}

function withEol(text, sample) {
  const lf = normalize(text);
  return sample !== null && sample.includes('\r\n') ? lf.replace(/\n/g, '\r\n') : lf;
}

// ---------------------------------------------------------------------------------------
// Git baseline

function git(args) {
  return execFileSync('git', args, { cwd: ROOT, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
}

let headFiles;
function headContent(rel) {
  if (headFiles === undefined) {
    try {
      headFiles = new Set(git(['ls-tree', '-r', '--name-only', 'HEAD']).split('\n').filter(Boolean));
    } catch {
      headFiles = new Set();
    }
  }
  if (!headFiles.has(rel)) return null;
  try {
    return git(['show', `HEAD:${rel}`]);
  } catch {
    return null;
  }
}

function statePath() {
  try {
    return path.resolve(ROOT, git(['rev-parse', '--git-path', 'agent-sync-state.json']).trim());
  } catch {
    return abs('.agent-sync-state.json');
  }
}

function loadState() {
  try {
    return JSON.parse(fs.readFileSync(statePath(), 'utf8'));
  } catch {
    return { files: {} };
  }
}

function saveState(state) {
  fs.writeFileSync(statePath(), JSON.stringify(state, null, 2));
}

// ---------------------------------------------------------------------------------------
// Frontmatter (flat YAML: top-level keys with optional indented continuation lines)

function parseFrontmatter(text) {
  const lf = normalize(text);
  const match = /^---\n([\s\S]*?)\n---\n?/.exec(lf);
  if (!match) return { entries: [], body: lf, hasFrontmatter: false };
  const entries = [];
  for (const line of match[1].split('\n')) {
    const key = /^([A-Za-z0-9_-]+):/.exec(line);
    if (key) entries.push({ key: key[1], lines: [line] });
    else if (entries.length) entries[entries.length - 1].lines.push(line);
  }
  return { entries, body: lf.slice(match[0].length), hasFrontmatter: true };
}

function entryValue(entry) {
  const value = [entry.lines[0].replace(/^[^:]+:\s*/, ''), ...entry.lines.slice(1)].join('\n').trim();
  const quoted = /^(["'])([\s\S]*)\1$/.exec(value);
  return quoted ? quoted[2] : value;
}

function renderFrontmatter(entries, body) {
  if (!entries.length) return body;
  return `---\n${entries.flatMap((e) => e.lines).join('\n')}\n---\n${body}`;
}

// ---------------------------------------------------------------------------------------
// Group kinds

const identical = {
  canonical: (text) => normalize(text),
  // Line-ending-only differences are ignored (git normalizes them), so this never causes churn.
  render: (sourceText) => sourceText,
};

function sharedFrontmatter(sharedKeys) {
  return {
    canonical(text) {
      const { entries, body } = parseFrontmatter(text);
      const shared = sharedKeys.map((k) => {
        const entry = entries.find((e) => e.key === k);
        return `${k}=${entry ? entryValue(entry) : ''}`;
      });
      return `${shared.join('\n')}\n\n${body}`;
    },
    render(sourceText, targetText) {
      const source = parseFrontmatter(sourceText);
      const target = targetText === null ? { entries: [] } : parseFrontmatter(targetText);
      let entries = target.entries.map((e) => ({ ...e }));
      for (const key of sharedKeys) {
        const from = source.entries.find((e) => e.key === key);
        const index = entries.findIndex((e) => e.key === key);
        if (!from) {
          if (index >= 0) entries.splice(index, 1);
        } else if (index < 0) {
          entries.push({ ...from });
        } else if (entryValue(entries[index]) !== entryValue(from)) {
          entries[index] = { ...from };
        }
      }
      // A new file only gets the shared keys, in the source's order.
      if (targetText === null) {
        entries = source.entries.filter((e) => sharedKeys.includes(e.key));
      }
      return withEol(renderFrontmatter(entries, source.body), targetText ?? sourceText);
    },
  };
}

// ---------------------------------------------------------------------------------------
// Discovering groups

function commandToPrompt(rel) {
  const inner = rel.slice('.claude/commands/'.length).replace(/\.md$/, '');
  return `.github/prompts/${inner.split('/').join('-')}.prompt.md`;
}

function promptToCommand(rel, existingCommands) {
  const existing = existingCommands.find((c) => commandToPrompt(c) === rel);
  if (existing) return existing;
  const name = path.posix.basename(rel).replace(/\.prompt\.md$/, '');
  const dash = name.indexOf('-');
  if (dash > 0) {
    const dir = name.slice(0, dash);
    if (fs.existsSync(abs(`.claude/commands/${dir}`))) return `.claude/commands/${dir}/${name.slice(dash + 1)}.md`;
  }
  return `.claude/commands/${name}.md`;
}

function discoverGroups(state) {
  const known = (prefix, suffix = '') =>
    Object.keys(state.files).filter((f) => f.startsWith(prefix) && f.endsWith(suffix));
  const unique = (list) => [...new Set(list)];
  const groups = [{ kind: identical, label: 'instructions', members: INSTRUCTION_FILES }];

  const skillFiles = unique([
    ...walk('.claude/skills'),
    ...walk('.github/skills'),
    ...known('.claude/skills/'),
    ...known('.github/skills/'),
  ]);
  const skillRels = unique(skillFiles.map((f) => f.replace(/^\.(claude|github)\/skills\//, '')));
  for (const rel of skillRels) {
    groups.push({ kind: identical, label: `skill ${rel}`, members: [`.claude/skills/${rel}`, `.github/skills/${rel}`] });
  }

  const commands = unique([...walk('.claude/commands', (f) => f.endsWith('.md')), ...known('.claude/commands/', '.md')]);
  const prompts = unique([
    ...walk('.github/prompts', (f) => f.endsWith('.prompt.md')),
    ...known('.github/prompts/', '.prompt.md'),
  ]);
  const commandPairs = new Map(commands.map((c) => [commandToPrompt(c), c]));
  for (const p of prompts) if (!commandPairs.has(p)) commandPairs.set(p, promptToCommand(p, commands));
  const commandKind = sharedFrontmatter(COMMAND_SHARED_KEYS);
  for (const [prompt, command] of commandPairs) {
    groups.push({ kind: commandKind, label: `command ${command}`, members: [command, prompt] });
  }

  const agentKind = sharedFrontmatter(AGENT_SHARED_KEYS);
  const agentNames = unique([
    ...[...walk('.claude/agents', (f) => f.endsWith('.md')), ...known('.claude/agents/', '.md')].map((f) =>
      path.posix.basename(f, '.md'),
    ),
    ...[...walk('.github/agents', (f) => f.endsWith('.agent.md')), ...known('.github/agents/', '.agent.md')].map((f) =>
      path.posix.basename(f, '.agent.md'),
    ),
  ]);
  for (const name of agentNames) {
    groups.push({ kind: agentKind, label: `agent ${name}`, members: [`.claude/agents/${name}.md`, `.github/agents/${name}.agent.md`] });
  }
  return groups;
}

// ---------------------------------------------------------------------------------------
// Sync

function syncGroup(group, state, options) {
  const { kind, members } = group;
  const current = Object.fromEntries(members.map((m) => [m, readFile(m)]));
  const baseline = Object.fromEntries(
    members.map((m) => [m, m in state.files ? state.files[m] : hash(headContent(m))]),
  );
  const changed = members.filter((m) => hash(current[m]) !== baseline[m]);
  const present = members.filter((m) => current[m] !== null);
  const equivalent = (a, b) => kind.canonical(current[a]) === kind.canonical(current[b]);
  const newest = (list) => [...list].sort((a, b) => mtime(b) - mtime(a))[0];
  const conflict = (reason) => ({ conflict: `${group.label}: ${reason}` });

  let source;
  const hinted = members.find((m) => options.from.has(m));
  if (hinted) {
    source = hinted;
  } else if (changed.length) {
    const edited = changed.filter((m) => current[m] !== null);
    const deleted = changed.filter((m) => current[m] === null);
    if (edited.length && deleted.length) {
      if (!options.preferNewest) return conflict(`${edited.join(', ')} changed but ${deleted.join(', ')} was deleted`);
      source = newest(edited);
    } else if (edited.length) {
      const differing = edited.some((m) => !equivalent(m, edited[0]));
      if (differing && !options.preferNewest) return conflict(`${edited.join(' and ')} were both changed differently`);
      source = differing ? newest(edited) : edited[0];
    } else {
      source = deleted[0];
    }
  } else if (present.length) {
    const differing = present.some((m) => !equivalent(m, present[0]));
    if (differing && !options.preferNewest) return conflict(`${present.join(' and ')} differ, and neither changed since the last sync`);
    source = differing ? newest(present) : present[0];
  } else {
    return { actions: [] };
  }

  const actions = [];
  for (const target of members) {
    if (target === source) continue;
    if (current[source] === null) {
      if (current[target] !== null) actions.push({ type: 'delete', target, source });
      continue;
    }
    const rendered = kind.render(current[source], current[target]);
    if (current[target] === null || normalize(rendered) !== normalize(current[target])) {
      actions.push({ type: current[target] === null ? 'create' : 'update', target, source, text: rendered });
    }
  }

  if (!options.check) {
    for (const action of actions) {
      if (action.type === 'delete') removeFile(action.target);
      else writeFile(action.target, action.text);
    }
    for (const m of members) {
      const text = readFile(m);
      if (text === null) delete state.files[m];
      else state.files[m] = hash(text);
    }
  }
  return { actions };
}

function run(options) {
  const state = loadState();
  const actions = [];
  const conflicts = [];
  for (const group of discoverGroups(state)) {
    if (options.onlyHinted && !group.members.some((m) => options.from.has(m))) continue;
    const result = syncGroup(group, state, options);
    if (result.conflict) conflicts.push(result.conflict);
    else actions.push(...result.actions);
  }
  if (!options.check) saveState(state);
  return { actions, conflicts };
}

const describe = (a) => `${a.type} ${a.target} (from ${a.source})`;

// ---------------------------------------------------------------------------------------
// Hook mode: collect every path-like string from the tool input and use managed ones as hints.

// Resolves symlinks and 8.3 short names so differently spelled paths compare equal.
function realPath(p) {
  try {
    return fs.realpathSync.native(p);
  } catch {
    const parent = path.dirname(p);
    return parent === p ? p : path.join(realPath(parent), path.basename(p));
  }
}

const REAL_ROOT = realPath(ROOT);

function repoRelative(candidate) {
  const resolved = realPath(path.resolve(ROOT, candidate));
  const fold = (p) => (process.platform === 'win32' ? p.toLowerCase() : p);
  const rel = path.relative(fold(REAL_ROOT), fold(resolved));
  if (!rel || rel.startsWith('..') || path.isAbsolute(rel)) return null;
  // Take the original casing from the resolved path, not the folded one.
  return toPosix(resolved.slice(resolved.length - rel.length));
}

function pathsInToolInput(value, found = []) {
  if (typeof value === 'string') {
    for (const candidate of value.split(/\r?\n/)) {
      const trimmed = candidate.trim();
      if (!trimmed || trimmed.length > 400) continue;
      const rel = repoRelative(trimmed);
      if (rel) found.push(rel);
    }
  } else if (Array.isArray(value)) {
    value.forEach((v) => pathsInToolInput(v, found));
  } else if (value && typeof value === 'object') {
    Object.values(value).forEach((v) => pathsInToolInput(v, found));
  }
  return found;
}

function isManaged(rel) {
  return (
    INSTRUCTION_FILES.includes(rel) ||
    /^\.(claude|github)\/skills\//.test(rel) ||
    /^\.claude\/(commands|agents)\/.+\.md$/.test(rel) ||
    /^\.github\/prompts\/.+\.prompt\.md$/.test(rel) ||
    /^\.github\/agents\/.+\.agent\.md$/.test(rel)
  );
}

async function runHook() {
  let input = {};
  try {
    const chunks = [];
    for await (const chunk of process.stdin) chunks.push(chunk);
    input = JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}');
  } catch {
    // No or invalid input: fall through to a full sync.
  }
  const event = input.hook_event_name ?? input.hookEventName ?? '';
  const hints = pathsInToolInput(input.tool_input ?? input.toolInput ?? {}).filter(isManaged);

  // Copilot runs every PostToolUse hook regardless of matcher; stay quiet for unrelated tools.
  if (event === 'PostToolUse' && !hints.length) return 0;

  // After an edit only the edited files' groups are synced; Stop sweeps everything.
  const { actions, conflicts } = run({
    from: new Set(hints),
    onlyHinted: event === 'PostToolUse',
    preferNewest: false,
    check: false,
  });

  if (conflicts.length) {
    if (event === 'Stop' && (input.stop_hook_active || input.stopHookActive)) return 0;
    process.stderr.write(
      `Claude Code / Copilot configuration is out of sync and needs a decision:\n` +
        conflicts.map((c) => `- ${c}`).join('\n') +
        `\nFollow the sync-agent-config skill: pick the correct version, then run ` +
        `\`node scripts/sync-agent-config.mjs --from <that file>\`.\n`,
    );
    return 2;
  }
  if (actions.length) {
    const message = `Agent config sync: ${actions.map(describe).join('; ')}`;
    const output = event === 'Stop'
      ? { systemMessage: message }
      : { hookSpecificOutput: { hookEventName: event || 'PostToolUse', additionalContext: message } };
    process.stdout.write(JSON.stringify(output));
  }
  return 0;
}

// ---------------------------------------------------------------------------------------
// CLI

function runCli(argv) {
  const options = { from: new Set(), preferNewest: false, check: false };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--check' || arg === '--dry-run') options.check = true;
    else if (arg === '--prefer-newest') options.preferNewest = true;
    else if (arg === '--from') options.from.add(repoRelative(path.resolve(argv[++i] ?? '.')) ?? argv[i]);
    else {
      console.error(`Unknown argument: ${arg}`);
      return 64;
    }
  }
  for (const f of options.from) {
    if (!isManaged(f)) {
      console.error(`--from ${f} is not a synced file`);
      return 64;
    }
  }

  const { actions, conflicts } = run(options);
  const verb = options.check ? 'would ' : '';
  for (const a of actions) console.log(`${verb}${describe(a)}`);
  for (const c of conflicts) console.error(`CONFLICT ${c}`);
  if (conflicts.length) {
    console.error('Resolve with --from <file> (the version to keep) or --prefer-newest.');
    return 1;
  }
  if (!actions.length) console.log('Claude Code and Copilot configuration are in sync.');
  return options.check && actions.length ? 1 : 0;
}

const argv = process.argv.slice(2);
process.exitCode = argv[0] === '--hook' ? await runHook() : runCli(argv);
