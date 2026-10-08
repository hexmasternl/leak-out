// The website's leaderboard: loads the top 25 scores of a level from the score server's public endpoint
// (GET /api/public/levels/{level}/scores/top) and shows them, with loading, empty and error states.
// The state logic is DOM-free and exported for the unit tests in website/tests; the page wiring at the
// bottom only runs in a browser.

export const MAX_ENTRIES = 25;

// The score server can take a while on a cold start, so the loading state waits this long before the error state.
export const REQUEST_TIMEOUT_MS = 20000;

/** The level number `value` asks for, clamped to 1..levelCount; 1 when it is not a number. */
export function clampLevel(value, levelCount) {
  const level = Math.trunc(Number(value));
  if (!Number.isFinite(level) || level < 1) return 1;
  return Math.min(level, Math.max(1, levelCount));
}

/** The public top scores URL of a level on the given server. */
export function topScoresUrl(server, level) {
  return `${server.replace(/\/+$/, "")}/api/public/levels/${level}/scores/top`;
}

/**
 * The entries of a top scores response, best first and at most 25, each as
 * { position, displayName, score, achievedAt }. `achievedAt` is the Date the server received the score, or
 * null when the response has no valid moment. Throws when the response does not have the expected shape.
 */
export function parseTopScores(body) {
  if (!body || !Array.isArray(body.entries)) throw new Error("Unexpected top scores response");
  return body.entries
    .filter((e) => e && Number.isFinite(e.position) && Number.isFinite(e.score))
    .map((e) => ({
      position: e.position,
      displayName: String(e.displayName ?? ""),
      score: e.score,
      achievedAt: parseMoment(e.achievedAt),
    }))
    .sort((a, b) => a.position - b.position)
    .slice(0, MAX_ENTRIES);
}

function parseMoment(value) {
  if (typeof value !== "string") return null;
  const moment = new Date(value);
  return Number.isNaN(moment.getTime()) ? null : moment;
}

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

/**
 * How long ago `achievedAt` was at `now`, in the page's language and the largest whole unit that fits:
 * "5 minutes ago", "2 days ago", "1 year ago". Under a minute, and for a moment in the future (clocks
 * differ), it is "now". Months count as 30 days and years as 365.
 */
export function relativeTime(achievedAt, now, locale) {
  const age = now.getTime() - achievedAt.getTime();
  if (age < MINUTE) return new Intl.RelativeTimeFormat(locale, { numeric: "auto" }).format(0, "second");
  // "always" gives "1 year ago" rather than "last year", so every entry reads as an amount of time.
  const format = new Intl.RelativeTimeFormat(locale, { numeric: "always" });
  const days = Math.floor(age / DAY);
  if (days >= 365) return format.format(-Math.floor(days / 365), "year");
  if (days >= 30) return format.format(-Math.floor(days / 30), "month");
  if (days >= 7) return format.format(-Math.floor(days / 7), "week");
  if (days >= 1) return format.format(-days, "day");
  if (age >= HOUR) return format.format(-Math.floor(age / HOUR), "hour");
  return format.format(-Math.floor(age / MINUTE), "minute");
}

/** The full date and time of `achievedAt` in the page's language, in the visitor's time zone unless `timeZone` is given. */
export function fullDate(achievedAt, locale, timeZone) {
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short", timeZone }).format(achievedAt);
}

/**
 * The table rows of the entries, with the score formatted for the page's locale and when it was set: as
 * relative time at `now` (`when`), as full date (`whenFull`) and as ISO moment (`whenIso`). All three are
 * empty for an entry without a moment.
 */
export function toRows(entries, locale, now = new Date()) {
  const format = new Intl.NumberFormat(locale);
  return entries.map((e) => ({
    position: String(e.position),
    displayName: e.displayName,
    score: format.format(e.score),
    when: e.achievedAt ? relativeTime(e.achievedAt, now, locale) : "",
    whenFull: e.achievedAt ? fullDate(e.achievedAt, locale) : "",
    whenIso: e.achievedAt ? e.achievedAt.toISOString() : "",
  }));
}

/**
 * Loads levels into a view. `view` has showLoading(level), showEntries(level, rows), showEmpty(level) and
 * showError(level). Only the latest load updates the view, so a slow answer for an earlier level never
 * replaces a newer one.
 */
export function createLeaderboard({ server, levelCount, locale, view, fetchImpl, timeoutMs = REQUEST_TIMEOUT_MS }) {
  let current = 0;
  let level = 1;

  async function load(requested) {
    level = clampLevel(requested, levelCount);
    const request = ++current;
    const shownLevel = level;
    view.showLoading(shownLevel);

    const controller = typeof AbortController === "function" ? new AbortController() : null;
    const timer = setTimeout(() => controller?.abort(), timeoutMs);
    try {
      const response = await fetchImpl(topScoresUrl(server, shownLevel), {
        headers: { Accept: "application/json" },
        signal: controller?.signal,
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const entries = parseTopScores(await response.json());
      if (request !== current) return;
      if (entries.length === 0) view.showEmpty(shownLevel);
      else view.showEntries(shownLevel, toRows(entries, locale));
    } catch {
      if (request !== current) return;
      view.showError(shownLevel);
    } finally {
      clearTimeout(timer);
    }
  }

  return {
    load,
    retry: () => load(level),
    previous: () => load(level - 1),
    next: () => load(level + 1),
    get level() {
      return level;
    },
  };
}

/** A view that renders into the leaderboard markup of layouts/leaderboard.html. */
export function createDomView(doc, strings) {
  const input = doc.getElementById("level-input");
  const caption = doc.getElementById("leaderboard-caption");
  const status = doc.getElementById("leaderboard-status");
  const table = doc.getElementById("leaderboard-table");
  const body = doc.getElementById("leaderboard-body");
  const previous = doc.getElementById("level-previous");
  const next = doc.getElementById("level-next");
  const levelCount = Number(input.max);
  let onRetry = () => {};

  function showLevel(level) {
    input.value = String(level);
    caption.textContent = `${strings.topScoresOf} ${level}`;
    previous.disabled = level <= 1;
    next.disabled = level >= levelCount;
  }

  function setStatus(text, isError) {
    status.className = isError ? "leaderboard-status is-error" : "leaderboard-status";
    status.replaceChildren();
    if (!text) return;
    const message = doc.createElement("p");
    message.textContent = text;
    status.append(message);
    if (isError) {
      const button = doc.createElement("button");
      button.type = "button";
      button.className = "button button-secondary";
      button.textContent = strings.tryAgain;
      button.addEventListener("click", () => onRetry());
      status.append(button);
    }
  }

  return {
    set onRetry(handler) {
      onRetry = handler;
    },
    showLoading(level) {
      showLevel(level);
      body.replaceChildren();
      table.hidden = true;
      setStatus(strings.loading, false);
    },
    showEntries(level, rows) {
      showLevel(level);
      setStatus("", false);
      body.replaceChildren(
        ...rows.map((row) => {
          const tr = doc.createElement("tr");
          for (const [text, className] of [[row.position, "rank"], [row.displayName, "player"], [row.score, "num"]]) {
            const td = doc.createElement("td");
            td.className = className;
            td.textContent = text;
            tr.append(td);
          }
          const when = doc.createElement("td");
          when.className = "when";
          if (row.whenIso) {
            const time = doc.createElement("time");
            time.setAttribute("datetime", row.whenIso);
            time.setAttribute("title", row.whenFull);
            time.textContent = row.when;
            when.append(time);
          }
          tr.append(when);
          return tr;
        }),
      );
      table.hidden = false;
    },
    showEmpty(level) {
      showLevel(level);
      body.replaceChildren();
      table.hidden = true;
      setStatus(strings.empty, false);
    },
    showError(level) {
      showLevel(level);
      body.replaceChildren();
      table.hidden = true;
      setStatus(strings.error, true);
    },
  };
}

function start() {
  const root = document.getElementById("leaderboard");
  if (!root) return;
  const strings = JSON.parse(root.dataset.strings);
  const view = createDomView(document, strings);
  const leaderboard = createLeaderboard({
    server: root.dataset.server,
    levelCount: Number(root.dataset.levelCount),
    locale: root.dataset.locale,
    view,
    fetchImpl: (url, options) => fetch(url, options),
  });
  view.onRetry = () => leaderboard.retry();

  document.getElementById("level-chooser").addEventListener("submit", (event) => {
    event.preventDefault();
    leaderboard.load(document.getElementById("level-input").value);
  });
  document.getElementById("level-previous").addEventListener("click", () => leaderboard.previous());
  document.getElementById("level-next").addEventListener("click", () => leaderboard.next());

  leaderboard.load(1);
}

if (typeof document !== "undefined") start();
