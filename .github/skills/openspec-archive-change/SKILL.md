---
name: openspec-archive-change
description: Archive a completed change in the experimental workflow. Use when the user wants to finalize and archive a change after implementation is complete.
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.2.0"
---

Archive a completed change in the experimental workflow.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **If no change name provided, prompt for selection**

   Run `openspec list --json` to get available changes. Use the **AskUserQuestion tool** to let the user select.

   Show only active changes (not already archived).
   Include the schema used for each change if available.

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Check artifact completion status**

   Run `openspec status --change "<name>" --json` to check artifact completion.

   Parse the JSON to understand:
   - `schemaName`: The workflow being used
   - `artifacts`: List of artifacts with their status (`done` or other)

   **If any artifacts are not `done`:**
   - Display warning listing incomplete artifacts
   - Use **AskUserQuestion tool** to confirm user wants to proceed
   - Proceed if user confirms

3. **Check task completion status**

   Read the tasks file (typically `tasks.md`) to check for incomplete tasks.

   Count tasks marked with `- [ ]` (incomplete) vs `- [x]` (complete).

   **If incomplete tasks found:**
   - Display warning showing count of incomplete tasks
   - Use **AskUserQuestion tool** to confirm user wants to proceed
   - Proceed if user confirms

   **If no tasks file exists:** Proceed without task-related warning.

4. **Assess delta spec sync state**

   Check for delta specs at `openspec/changes/<name>/specs/`. If none exist, proceed without sync prompt.

   **If delta specs exist:**
   - Compare each delta spec with its corresponding main spec at `openspec/specs/<capability>/spec.md`
   - Determine what changes would be applied (adds, modifications, removals, renames)
   - Show a combined summary before prompting

   **Prompt options:**
   - If changes needed: "Sync now (recommended)", "Archive without syncing"
   - If already synced: "Archive now", "Sync anyway", "Cancel"

   If user chooses sync, use Task tool (subagent_type: "general-purpose", prompt: "Use Skill tool to invoke openspec-sync-specs for change '<name>'. Delta spec analysis: <include the analyzed delta spec summary>"). Proceed to archive regardless of choice.

5. **Find a linked GitHub issue**

   Before moving anything, look for a GitHub issue the change links to. Search the change's artifacts (`proposal.md` first, then `design.md`, `tasks.md` and any other markdown in `openspec/changes/<name>/`) for:
   - a `Tracking issue:` line, the convention in this repo, e.g. `Tracking issue: [#36 First time user experience](https://github.com/hexmasternl/leak-out/issues/36)`
   - otherwise, any GitHub issue URL (`https://github.com/<owner>/<repo>/issues/<number>`)

   Take the issue number and `<owner>/<repo>` from the URL. A bare `#<number>` without a URL refers to this repository. Only treat an issue as *the* linked issue when it is the change's tracking issue: an issue merely mentioned in passing (a related or coordinating change) is not closed. If several candidates remain and it is unclear which one this change tracks, use the **AskUserQuestion tool** to let the user pick (or none).

   Remember the issue for step 7. If no issue is linked, skip step 7.

6. **Perform the archive**

   Create the archive directory if it doesn't exist:
   ```bash
   mkdir -p openspec/changes/archive
   ```

   Generate target name using current date: `YYYY-MM-DD-<change-name>`

   **Check if target already exists:**
   - If yes: Fail with error, suggest renaming existing archive or using different date
   - If no: Move the change directory to archive

   ```bash
   mv openspec/changes/<name> openspec/changes/archive/YYYY-MM-DD-<name>
   ```

7. **Close the linked GitHub issue**

   Only when step 5 found a linked issue.

   **The change counts as completed** when all artifacts are `done` and all tasks are complete. If the archive went ahead with warnings (incomplete artifacts or tasks), do NOT close the issue automatically: use the **AskUserQuestion tool** to ask whether to close it anyway, and leave it open unless the user confirms.

   Check the issue's state first:
   ```bash
   gh issue view <number> --repo <owner>/<repo> --json state,title
   ```
   - Already `CLOSED`: do nothing, just report it.
   - `OPEN`: close it as completed, referencing the archive:
     ```bash
     gh issue close <number> --repo <owner>/<repo> --reason completed --comment "Completed by OpenSpec change <name>, archived to openspec/changes/archive/YYYY-MM-DD-<name>/."
     ```

   If `gh` is missing, not authenticated, or the call fails, do not undo the archive: report the failure and the command the user can run themselves.

8. **Display summary**

   Show archive completion summary including:
   - Change name
   - Schema that was used
   - Archive location
   - Whether specs were synced (if applicable)
   - Note about any warnings (incomplete artifacts/tasks)
   - The linked GitHub issue and what happened to it (closed / already closed / left open / failed), if any

**Output On Success**

```
## Archive Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** openspec/changes/archive/YYYY-MM-DD-<name>/
**Specs:** ✓ Synced to main specs (or "No delta specs" or "Sync skipped")
**Issue:** #<number> closed (or "already closed", "left open", "No linked issue")

All artifacts complete. All tasks complete.
```

**Guardrails**
- Only close the change's own tracking issue, never issues merely mentioned in the artifacts
- Never close a linked issue automatically when the archive had incomplete artifacts or tasks; ask first
- A failure to close the issue never blocks or reverts the archive
- Always prompt for change selection if not provided
- Use artifact graph (openspec status --json) for completion checking
- Don't block archive on warnings - just inform and confirm
- Preserve .openspec.yaml when moving to archive (it moves with the directory)
- Show clear summary of what happened
- If sync is requested, use openspec-sync-specs approach (agent-driven)
- If delta specs exist, always run the sync assessment and show the combined summary before prompting
