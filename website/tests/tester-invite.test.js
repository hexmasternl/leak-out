import { test, describe } from "node:test";
import assert from "node:assert/strict";
import {
  AUTO_OPEN_DELAY_MS,
  DISMISSED_KEY,
  isDismissed,
  rememberDismissal,
  createTesterInvite,
} from "../src/assets/js/tester-invite.js";

/** A stand-in for a native <dialog>: showModal() opens it, close() closes it and fires "close". */
function fakeDialog() {
  const listeners = {};
  const dialog = {
    open: false,
    shown: 0,
    addEventListener(type, handler) {
      (listeners[type] ??= []).push(handler);
    },
    dispatch(type, event = {}) {
      for (const handler of listeners[type] ?? []) handler({ type, ...event });
    },
    showModal() {
      if (dialog.open) throw new Error("InvalidStateError: the dialog is already open");
      dialog.open = true;
      dialog.shown += 1;
    },
    close() {
      if (!dialog.open) return;
      dialog.open = false;
      dialog.dispatch("close");
    },
  };
  return dialog;
}

function memoryStorage(initial = {}) {
  const values = { ...initial };
  return {
    values,
    getItem: (key) => (key in values ? values[key] : null),
    setItem: (key, value) => (values[key] = String(value)),
  };
}

const throwingStorage = {
  getItem() {
    throw new Error("SecurityError");
  },
  setItem() {
    throw new Error("QuotaExceededError");
  },
};

/** Timers that only run when the test says so. */
function manualTimers() {
  const pending = new Map();
  let next = 1;
  return {
    pending,
    setTimer: (callback, delay) => {
      const id = next++;
      pending.set(id, { callback, delay });
      return id;
    },
    clearTimer: (id) => pending.delete(id),
    runAll() {
      for (const [id, { callback }] of [...pending]) {
        pending.delete(id);
        callback();
      }
    },
  };
}

function inviteWith({ storage = memoryStorage(), dialog = fakeDialog(), focused = null } = {}) {
  const timers = manualTimers();
  const invite = createTesterInvite({
    dialog,
    storage,
    setTimer: timers.setTimer,
    clearTimer: timers.clearTimer,
    activeElement: () => focused,
  });
  return { invite, dialog, storage, timers };
}

describe("isDismissed and rememberDismissal", () => {
  test("a dismissal is stored under the documented key", () => {
    const storage = memoryStorage();
    assert.equal(isDismissed(storage), false);
    rememberDismissal(storage);
    assert.equal(storage.values[DISMISSED_KEY], "1");
    assert.equal(isDismissed(storage), true);
  });

  test("throwing or missing storage counts as not dismissed and does not throw", () => {
    assert.equal(isDismissed(throwingStorage), false);
    assert.equal(isDismissed(null), false);
    assert.doesNotThrow(() => rememberDismissal(throwingStorage));
    assert.doesNotThrow(() => rememberDismissal(null));
  });
});

describe("createTesterInvite", () => {
  test("opens on its own after 15 seconds when never dismissed", () => {
    const { invite, dialog, timers } = inviteWith();
    assert.equal(invite.scheduleAutoOpen(), true);
    assert.deepEqual([...timers.pending.values()].map((t) => t.delay), [AUTO_OPEN_DELAY_MS]);
    assert.equal(dialog.open, false);
    timers.runAll();
    assert.equal(dialog.open, true);
  });

  test("does not open on its own after a dismissal", () => {
    const { invite, dialog, timers } = inviteWith({ storage: memoryStorage({ [DISMISSED_KEY]: "1" }) });
    assert.equal(invite.scheduleAutoOpen(), false);
    assert.equal(timers.pending.size, 0);
    timers.runAll();
    assert.equal(dialog.open, false);
  });

  test("still opens from a trigger after a dismissal", () => {
    const { invite, dialog } = inviteWith({ storage: memoryStorage({ [DISMISSED_KEY]: "1" }) });
    invite.open();
    assert.equal(dialog.open, true);
  });

  test("is not opened a second time when the visitor already opened it", () => {
    const { invite, dialog, timers } = inviteWith();
    invite.scheduleAutoOpen();
    invite.open();
    assert.equal(timers.pending.size, 0, "opening by hand cancels the timer");
    timers.runAll();
    invite.open();
    assert.equal(dialog.shown, 1);
  });

  test("the timer does not reopen a dialog that is open", () => {
    const dialog = fakeDialog();
    const { invite, timers } = inviteWith({ dialog });
    invite.scheduleAutoOpen();
    dialog.showModal(); // opened some other way while the timer was pending
    assert.doesNotThrow(() => timers.runAll());
    assert.equal(dialog.shown, 1);
  });

  test("leaving early cancels the automatic opening", () => {
    const { invite, dialog, timers } = inviteWith();
    invite.scheduleAutoOpen();
    invite.cancelAutoOpen();
    timers.runAll();
    assert.equal(dialog.open, false);
  });

  test("any close records the dismissal", () => {
    const { invite, storage } = inviteWith();
    invite.open();
    invite.close();
    assert.equal(storage.values[DISMISSED_KEY], "1");
  });

  test("Escape (a native close) records the dismissal", () => {
    const { invite, dialog, storage } = inviteWith();
    invite.open();
    dialog.close(); // what the browser does on Escape
    assert.equal(isDismissed(storage), true);
  });

  test("a click on the backdrop closes it, a click inside does not", () => {
    const { invite, dialog } = inviteWith();
    invite.open();
    dialog.dispatch("click", { target: { tagName: "A" } });
    assert.equal(dialog.open, true);
    dialog.dispatch("click", { target: dialog });
    assert.equal(dialog.open, false);
  });

  test("closing returns focus to the trigger that opened it", () => {
    const { invite } = inviteWith();
    let focused = 0;
    const trigger = { focus: () => (focused += 1) };
    invite.open(trigger);
    invite.close();
    assert.equal(focused, 1);
  });

  test("an automatic opening returns focus to what had focus before", () => {
    let focused = 0;
    const { invite, timers } = inviteWith({ focused: { focus: () => (focused += 1) } });
    invite.scheduleAutoOpen();
    timers.runAll();
    invite.close();
    assert.equal(focused, 1);
  });

  test("with throwing storage it still opens, closes and opens on its own", () => {
    const { invite, dialog, timers } = inviteWith({ storage: throwingStorage });
    assert.equal(invite.scheduleAutoOpen(), true);
    timers.runAll();
    assert.equal(dialog.open, true);
    assert.doesNotThrow(() => invite.close());
    assert.equal(dialog.open, false);
    invite.open();
    assert.equal(dialog.open, true);
  });
});
