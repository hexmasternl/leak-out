// A minimal stand-in for the few DOM APIs the leaderboard view uses, so it can be tested in Node.

class FakeElement {
  constructor(tag, attributes = {}) {
    this.tagName = tag.toUpperCase();
    this.children = [];
    this.attributes = { ...attributes };
    this.listeners = {};
    this.textContent = "";
    this.className = "";
    this.hidden = false;
    this.disabled = false;
    this.value = attributes.value ?? "";
    this.max = attributes.max ?? "";
  }
  append(...nodes) {
    this.children.push(...nodes);
  }
  replaceChildren(...nodes) {
    this.children = [...nodes];
  }
  setAttribute(name, value) {
    this.attributes[name] = String(value);
  }
  removeAttribute(name) {
    delete this.attributes[name];
  }
  addEventListener(type, handler) {
    (this.listeners[type] ??= []).push(handler);
  }
  click() {
    for (const handler of this.listeners.click ?? []) handler({ type: "click" });
  }
}

export function fakeLeaderboardDocument(levelCount = 96) {
  const elements = {
    "level-input": new FakeElement("input", { value: "1", max: String(levelCount) }),
    "leaderboard-caption": new FakeElement("h2"),
    "leaderboard-status": new FakeElement("div"),
    "leaderboard-table": new FakeElement("table"),
    "leaderboard-body": new FakeElement("tbody"),
    "level-previous": new FakeElement("button"),
    "level-next": new FakeElement("button"),
  };
  return {
    elements,
    getElementById: (id) => elements[id] ?? null,
    createElement: (tag) => new FakeElement(tag),
  };
}
