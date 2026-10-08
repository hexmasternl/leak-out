import { test, describe } from "node:test";
import assert from "node:assert/strict";
import {
  MAX_ENTRIES,
  clampLevel,
  topScoresUrl,
  parseTopScores,
  toRows,
  createLeaderboard,
  createDomView,
} from "../src/assets/js/leaderboard.js";
import { fakeLeaderboardDocument } from "./fake-dom.js";

const strings = {
  loading: "Loading scores…",
  empty: "Nobody has a score on this level yet. Be the first!",
  error: "The scores could not be loaded.",
  tryAgain: "Try again",
  topScoresOf: "Top 25 of level",
};

function scores(count) {
  return Array.from({ length: count }, (_, i) => ({ position: i + 1, displayName: `Player ${i + 1}`, score: 5000 - i * 10 }));
}

function jsonResponse(body, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

/** A view that records what it was asked to show. */
function recordingView() {
  const calls = [];
  return {
    calls,
    last: () => calls.at(-1),
    showLoading: (level) => calls.push({ state: "loading", level }),
    showEntries: (level, rows) => calls.push({ state: "entries", level, rows }),
    showEmpty: (level) => calls.push({ state: "empty", level }),
    showError: (level) => calls.push({ state: "error", level }),
  };
}

function leaderboardWith(fetchImpl, view = recordingView(), levelCount = 96) {
  const requested = [];
  const board = createLeaderboard({
    server: "https://scores.example/",
    levelCount,
    locale: "en",
    view,
    fetchImpl: (url, options) => {
      requested.push(url);
      return fetchImpl(url, options);
    },
  });
  return { board, view, requested };
}

describe("clampLevel", () => {
  test("keeps a level inside the library", () => assert.equal(clampLevel("12", 96), 12));
  test("raises levels below 1 to 1", () => {
    assert.equal(clampLevel(0, 96), 1);
    assert.equal(clampLevel(-5, 96), 1);
  });
  test("lowers levels above the library to the last level", () => assert.equal(clampLevel(120, 96), 96));
  test("drops a fraction", () => assert.equal(clampLevel("7.8", 96), 7));
  test("treats text and empty input as level 1", () => {
    assert.equal(clampLevel("abc", 96), 1);
    assert.equal(clampLevel("", 96), 1);
  });
});

describe("topScoresUrl", () => {
  test("builds the public endpoint without a double slash", () => {
    assert.equal(topScoresUrl("https://scores.example/", 12), "https://scores.example/api/public/levels/12/scores/top");
    assert.equal(topScoresUrl("https://scores.example", 3), "https://scores.example/api/public/levels/3/scores/top");
  });
});

describe("parseTopScores", () => {
  test("keeps at most 25 entries, best first", () => {
    const shuffled = scores(40).reverse();
    const entries = parseTopScores({ level: 3, entries: shuffled });
    assert.equal(entries.length, MAX_ENTRIES);
    assert.deepEqual(entries.map((e) => e.position), Array.from({ length: 25 }, (_, i) => i + 1));
    assert.equal(entries[0].score, 5000);
  });
  test("keeps only position, display name and score", () => {
    const [entry] = parseTopScores({ entries: [{ position: 1, displayName: "Ann", score: 120, playerId: "secret" }] });
    assert.deepEqual(entry, { position: 1, displayName: "Ann", score: 120 });
  });
  test("returns no entries for a level without scores", () => assert.deepEqual(parseTopScores({ level: 7, entries: [] }), []));
  test("rejects a response without entries", () => {
    assert.throws(() => parseTopScores({}));
    assert.throws(() => parseTopScores(null));
  });
});

describe("toRows", () => {
  test("formats scores with the page's digit grouping", () => {
    const entries = [{ position: 1, displayName: "Ann", score: 12500 }];
    assert.deepEqual(toRows(entries, "en"), [{ position: "1", displayName: "Ann", score: "12,500" }]);
    assert.equal(toRows(entries, "de")[0].score, "12.500");
  });
});

describe("createLeaderboard", () => {
  test("shows loading, then the entries of the level", async () => {
    const { board, view, requested } = leaderboardWith(async () => jsonResponse({ level: 1, entries: scores(3) }));
    await board.load(1);
    assert.deepEqual(requested, ["https://scores.example/api/public/levels/1/scores/top"]);
    assert.deepEqual(view.calls.map((c) => c.state), ["loading", "entries"]);
    assert.equal(view.last().rows.length, 3);
  });

  test("shows at most 25 entries of a level with 40 scores", async () => {
    const { board, view } = leaderboardWith(async () => jsonResponse({ level: 12, entries: scores(40) }));
    await board.load(12);
    assert.equal(view.last().level, 12);
    assert.equal(view.last().rows.length, 25);
    assert.equal(view.last().rows[0].position, "1");
    assert.equal(view.last().rows[24].position, "25");
  });

  test("shows the empty state for a level without scores", async () => {
    const { board, view } = leaderboardWith(async () => jsonResponse({ level: 7, entries: [] }));
    await board.load(7);
    assert.deepEqual(view.last(), { state: "empty", level: 7 });
  });

  test("shows the error state when the server does not answer", async () => {
    const { board, view } = leaderboardWith(async () => {
      throw new TypeError("Failed to fetch");
    });
    await board.load(4);
    assert.deepEqual(view.last(), { state: "error", level: 4 });
  });

  test("shows the error state on an error status", async () => {
    const { board, view } = leaderboardWith(async () => jsonResponse({ title: "Too many requests" }, 429));
    await board.load(4);
    assert.equal(view.last().state, "error");
  });

  test("shows the error state when the request times out", async () => {
    const view = recordingView();
    const board = createLeaderboard({
      server: "https://scores.example",
      levelCount: 96,
      locale: "en",
      view,
      timeoutMs: 10,
      fetchImpl: (_url, { signal }) =>
        new Promise((_resolve, reject) => signal.addEventListener("abort", () => reject(new Error("aborted")))),
    });
    await board.load(2);
    assert.deepEqual(view.last(), { state: "error", level: 2 });
  });

  test("try again loads the same level again", async () => {
    let attempts = 0;
    const { board, view, requested } = leaderboardWith(async () => {
      attempts += 1;
      if (attempts === 1) throw new TypeError("Failed to fetch");
      return jsonResponse({ level: 5, entries: scores(2) });
    });
    await board.load(5);
    assert.equal(view.last().state, "error");
    await board.retry();
    assert.equal(view.last().state, "entries");
    assert.equal(requested.at(-1), "https://scores.example/api/public/levels/5/scores/top");
  });

  test("previous and next move one level, within the library", async () => {
    const { board, requested } = leaderboardWith(async () => jsonResponse({ entries: [] }), recordingView(), 3);
    await board.load(1);
    await board.previous();
    assert.equal(board.level, 1);
    await board.next();
    await board.next();
    await board.next();
    assert.equal(board.level, 3);
    assert.deepEqual(
      requested.map((u) => u.match(/levels\/(\d+)/)[1]),
      ["1", "1", "2", "3", "3"],
    );
  });

  test("a slow answer for an earlier level does not replace a newer level", async () => {
    let releaseFirst;
    const { board, view } = leaderboardWith((url) => {
      if (url.includes("/levels/1/")) {
        return new Promise((resolve) => (releaseFirst = () => resolve(jsonResponse({ entries: scores(5) }))));
      }
      return Promise.resolve(jsonResponse({ entries: scores(2) }));
    });
    const first = board.load(1);
    await board.load(2);
    releaseFirst();
    await first;
    assert.equal(view.last().level, 2);
    assert.equal(view.last().rows.length, 2);
  });
});

describe("createDomView", () => {
  test("loading shows the level, the loading message and no rows", () => {
    const doc = fakeLeaderboardDocument();
    const view = createDomView(doc, strings);
    view.showLoading(12);
    const { elements: el } = doc;
    assert.equal(el["level-input"].value, "12");
    assert.equal(el["leaderboard-caption"].textContent, "Top 25 of level 12");
    assert.equal(el["leaderboard-status"].children[0].textContent, strings.loading);
    assert.equal(el["leaderboard-table"].hidden, true);
  });

  test("entries fill the table with rank, player and score", () => {
    const doc = fakeLeaderboardDocument();
    const view = createDomView(doc, strings);
    view.showEntries(3, toRows(scores(2), "en"));
    const { elements: el } = doc;
    assert.equal(el["leaderboard-table"].hidden, false);
    assert.equal(el["leaderboard-status"].children.length, 0);
    const cells = el["leaderboard-body"].children.map((tr) => tr.children.map((td) => td.textContent));
    assert.deepEqual(cells, [
      ["1", "Player 1", "5,000"],
      ["2", "Player 2", "4,990"],
    ]);
  });

  test("a display name is shown as text, never as markup", () => {
    const doc = fakeLeaderboardDocument();
    const view = createDomView(doc, strings);
    view.showEntries(1, [{ position: "1", displayName: "<img src=x onerror=alert(1)>", score: "10" }]);
    const nameCell = doc.elements["leaderboard-body"].children[0].children[1];
    assert.equal(nameCell.textContent, "<img src=x onerror=alert(1)>");
    assert.equal(nameCell.children.length, 0);
  });

  test("empty shows the empty message and hides the table", () => {
    const doc = fakeLeaderboardDocument();
    const view = createDomView(doc, strings);
    view.showEmpty(7);
    assert.equal(doc.elements["leaderboard-status"].children[0].textContent, strings.empty);
    assert.equal(doc.elements["leaderboard-table"].hidden, true);
  });

  test("error shows the message and a Try again button that retries", () => {
    const doc = fakeLeaderboardDocument();
    const view = createDomView(doc, strings);
    let retried = 0;
    view.onRetry = () => (retried += 1);
    view.showError(4);
    const [message, button] = doc.elements["leaderboard-status"].children;
    assert.equal(message.textContent, strings.error);
    assert.equal(button.textContent, strings.tryAgain);
    assert.match(doc.elements["leaderboard-status"].className, /is-error/);
    button.click();
    assert.equal(retried, 1);
  });

  test("previous is disabled on level 1 and next on the last level", () => {
    const doc = fakeLeaderboardDocument(96);
    const view = createDomView(doc, strings);
    view.showLoading(1);
    assert.equal(doc.elements["level-previous"].disabled, true);
    assert.equal(doc.elements["level-next"].disabled, false);
    view.showLoading(96);
    assert.equal(doc.elements["level-previous"].disabled, false);
    assert.equal(doc.elements["level-next"].disabled, true);
  });
});
