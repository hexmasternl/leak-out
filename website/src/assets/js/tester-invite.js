// The tester invitation: a native <dialog> (layouts/_partials/tester-invite.html) that invites visitors to the
// closed test on Google Play. It opens from every [data-tester-open] link, and on its own after 15 seconds on a
// page unless the visitor dismissed it before. A dismissal is remembered in localStorage, in this browser only.
// The logic takes the dialog, storage and timer as parameters so website/tests can run it without a browser;
// the page wiring at the bottom only runs in a browser. Remove it when the game leaves closed testing.

export const DISMISSED_KEY = "leakout.testerInvite.dismissed";

export const AUTO_OPEN_DELAY_MS = 15000;

/** Whether the visitor dismissed the invitation before; false when storage is missing or throws. */
export function isDismissed(storage) {
  try {
    return storage?.getItem(DISMISSED_KEY) === "1";
  } catch {
    return false;
  }
}

/** Remembers a dismissal; does nothing when storage is missing or throws. */
export function rememberDismissal(storage) {
  try {
    storage?.setItem(DISMISSED_KEY, "1");
  } catch {
    // The dialog keeps working; it may just open on its own again on a later page.
  }
}

/**
 * Controls the dialog. `dialog` has `open`, `showModal()`, `close()` and `addEventListener()`. Any close (the
 * close button, Escape or a click on the backdrop) counts as a dismissal and returns focus to where it was.
 */
export function createTesterInvite({
  dialog,
  storage,
  setTimer = setTimeout,
  clearTimer = clearTimeout,
  delayMs = AUTO_OPEN_DELAY_MS,
  activeElement = () => null,
}) {
  let timer = null;
  let returnFocusTo = null;

  function cancelAutoOpen() {
    if (timer !== null) clearTimer(timer);
    timer = null;
  }

  function open(returnTo = activeElement()) {
    cancelAutoOpen();
    if (dialog.open) return;
    returnFocusTo = returnTo;
    dialog.showModal();
  }

  dialog.addEventListener("close", () => {
    rememberDismissal(storage);
    const target = returnFocusTo;
    returnFocusTo = null;
    target?.focus?.();
  });

  // The dialog element itself is only the target of a click outside its content, on the backdrop.
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });

  return {
    open,
    close: () => dialog.close(),
    /** Opens the dialog after the delay, unless it was dismissed before; returns whether a timer was started. */
    scheduleAutoOpen() {
      cancelAutoOpen();
      if (isDismissed(storage)) return false;
      timer = setTimer(() => {
        timer = null;
        if (!dialog.open) open();
      }, delayMs);
      return true;
    },
    cancelAutoOpen,
  };
}

function browserStorage() {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

function start() {
  const dialog = document.getElementById("tester-invite");
  // Without dialog support the triggers stay ordinary links to the home page section.
  if (!dialog || typeof dialog.showModal !== "function") return;

  const invite = createTesterInvite({
    dialog,
    storage: browserStorage(),
    activeElement: () => document.activeElement,
  });

  for (const trigger of document.querySelectorAll("[data-tester-open]")) {
    trigger.addEventListener("click", (event) => {
      event.preventDefault();
      invite.open(trigger);
    });
  }
  for (const button of dialog.querySelectorAll("[data-tester-close]")) {
    button.addEventListener("click", () => invite.close());
  }

  invite.scheduleAutoOpen();
}

if (typeof document !== "undefined") start();
