# Why `includeShadowDom: true` Is Needed in Cypress

## The Problem

This Cypress test fails:

```typescript
cy.get('[data-cy="header-Date"]').find('button').click()
// ❌ AssertionError: Expected to find element: `button`, but never found it.
```

The `button` is there — you can see it in the browser, you can click it manually. But Cypress says it doesn't exist. Why?

---

## Background: What Is the DOM?

When a browser loads an HTML page, it builds a tree structure called the **DOM** (Document Object Model). Every HTML element becomes a node in this tree.

```
<html>
  <body>
    <div>
      <h1>Hello</h1>
      <p>World</p>
    </div>
  </body>
</html>
```

This becomes:

```
html
 └── body
      └── div
           ├── h1 → "Hello"
           └── p  → "World"
```

When you write `document.querySelector('h1')` in JavaScript, or `cy.get('h1')` in Cypress, they search this tree top-to-bottom and find the `<h1>`. This is called the **light DOM** — the normal, visible DOM tree that everyone can access.

---

## What Is Shadow DOM?

Shadow DOM is a browser feature that lets a component **hide its internal HTML** from the outside world. Think of it as a component putting its internals inside a locked box. Code outside the box cannot see or reach inside.

### A Real Example

Imagine a sortable table header cell. In our design system, the `<si-table-cell>` component needs to render:
- The label text you provide (e.g., "Date")
- A sort arrow icon (↕)
- A clickable button that wraps both

The component author wants to control the button and icon — they don't want outside CSS or JavaScript accidentally breaking them. So they put the button inside a **shadow root**.

### What the HTML Looks Like

You write this in your Angular template:

```html
<si-table-cell data-cy="header-Date" si-sort-key="date">
  Date
</si-table-cell>
```

But the browser actually renders this:

```
<si-table-cell data-cy="header-Date">
  ┌─── #shadow-root (closed/open) ───────────────────┐
  │                                                    │
  │   <button class="button">        ← HIDDEN inside  │
  │     <div class="text">                             │
  │       <slot></slot>              ← "Date" appears  │
  │     </div>                         here via slot   │
  │     <si-icon>↕</si-icon>                           │
  │   </button>                                        │
  │                                                    │
  └────────────────────────────────────────────────────┘
  "Date"                             ← light DOM child
</si-table-cell>
```

The key insight: **the `<button>` does not exist in the light DOM**. It only exists inside the shadow root. From the outside, it's invisible.

### The Slot Mechanism

You might wonder: "If the button is hidden, how does my 'Date' text get inside it?"

The answer is **slots**. The `<slot></slot>` element acts like a window — it takes the light DOM children ("Date") and projects them visually into the shadow DOM layout. The text stays in the light DOM, but it *appears* inside the button visually.

```
What you see in the browser:

  ┌──────────────────────────┐
  │ Date                   ↕ │  ← looks like one clickable button
  └──────────────────────────┘

What the DOM actually is:

  Light DOM:    <si-table-cell> "Date" </si-table-cell>
  Shadow DOM:   <button> <slot/> <si-icon/> </button>
```

---

## Why Angular Doesn't Have This Problem (Normally)

Angular uses something called **ViewEncapsulation.Emulated** by default. This means Angular scopes CSS using generated attributes (like `_ngcontent-abc123`), but it does **NOT** use real shadow DOM. All Angular component HTML lives in the normal light DOM tree.

```html
<!-- Angular component (emulated encapsulation) -->
<app-my-component _nghost-abc123>
  <div _ngcontent-abc123>          ← still in light DOM, visible to Cypress
    <button _ngcontent-abc123>Click me</button>
  </div>
</app-my-component>
```

Cypress can find this `<button>` with no problem because it's in the light DOM.

---

## Why the Design System v5 Has This Problem

The Signal Iduna Design System v5 is built with **Lit**, a framework for building **web components**. Web components are a browser standard (not Angular-specific) and they use **real shadow DOM** by default.

When you use `<si-table-cell>`, `<si-button>`, `<si-header>` etc., you are using Lit web components. Angular treats them as custom HTML elements and passes data to them via attributes/properties, but their **internal rendering is hidden inside shadow DOM**.

### The Two Layers in Our App

```
┌─────────────────────────────────────────────────────────┐
│  Angular Application (light DOM)                        │
│                                                         │
│  <app-root>                                             │
│    <si-table>                    ← web component        │
│      <si-table-header>           ← web component        │
│        <si-table-row>            ← web component        │
│          <si-table-cell           ← web component       │
│            data-cy="header-Date"                        │
│            si-sort-key="date">                          │
│            Date                   ← light DOM text      │
│          </si-table-cell>                               │
│                                                         │
│          Inside si-table-cell's shadow root:             │
│          ┌──────────────────────────────────────┐       │
│          │  <button class="button">   ← HIDDEN  │       │
│          │    <slot></slot>                      │       │
│          │    <si-icon>↕</si-icon>               │       │
│          │  </button>                            │       │
│          └──────────────────────────────────────┘       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## How Cypress Searches the DOM

### Default Behavior (without `includeShadowDom`)

By default, Cypress searches **only the light DOM**. When you write:

```typescript
cy.get('[data-cy="header-Date"]')     // ✅ works — data-cy is on the light DOM element
  .find('button')                      // ❌ fails — no <button> in light DOM
  .click()
```

Step by step:
1. `cy.get('[data-cy="header-Date"]')` — searches the light DOM for an element with `data-cy="header-Date"`. Finds `<si-table-cell>`. ✅
2. `.find('button')` — searches **inside** `<si-table-cell>` for a `<button>`. But it only looks in the light DOM children. The only light DOM child is the text "Date". There is no `<button>` in the light DOM. ❌

Cypress waits 4 seconds (the default timeout), retrying over and over, and finally gives up:

```
AssertionError: Timed out retrying after 4000ms:
Expected to find element: `button`, but never found it.
```

### With `includeShadowDom: true`

When you add `includeShadowDom: true` to `cypress.config.ts`, Cypress changes its search behavior. Now when it searches for elements, it also looks **inside shadow roots**.

```typescript
cy.get('[data-cy="header-Date"]')     // ✅ finds <si-table-cell>
  .find('button')                      // ✅ pierces into shadow root, finds <button>
  .click()                             // ✅ clicks the sort button
```

Step by step:
1. `cy.get('[data-cy="header-Date"]')` — same as before, finds `<si-table-cell>`. ✅
2. `.find('button')` — searches inside `<si-table-cell>`. First checks light DOM children (no button). Then checks shadow root children — finds `<button class="button">`. ✅
3. `.click()` — clicks it. ✅

---

## Visual Comparison

```
WITHOUT includeShadowDom:

  Cypress search scope:
  ┌────────────────────────────────────────┐
  │  <si-table-cell>                       │
  │    "Date"                ← searched ✅ │
  │                                        │
  │    #shadow-root          ← IGNORED ⛔  │
  │    ┌──────────────────┐                │
  │    │ <button>          │  ← invisible  │
  │    │   <slot/>         │               │
  │    │   <si-icon/>      │               │
  │    │ </button>         │               │
  │    └──────────────────┘                │
  │                                        │
  └────────────────────────────────────────┘
  Result: "button not found" ❌


WITH includeShadowDom: true:

  Cypress search scope:
  ┌────────────────────────────────────────┐
  │  <si-table-cell>                       │
  │    "Date"                ← searched ✅ │
  │                                        │
  │    #shadow-root          ← searched ✅ │
  │    ┌──────────────────┐                │
  │    │ <button>          │  ← FOUND ✅   │
  │    │   <slot/>         │               │
  │    │   <si-icon/>      │               │
  │    │ </button>         │               │
  │    └──────────────────┘                │
  │                                        │
  └────────────────────────────────────────┘
  Result: button found and clicked ✅
```

---

## Why It Passed on Jenkins But Failed Locally

On Jenkins (Cypress 15.13.1), all 7 tests passed. On the remote machine (Cypress 15.12.0), the sort test failed. Two possible reasons:

1. **Cypress version difference**: Minor versions can have different shadow DOM handling behavior. 15.13.1 may have a fix or change that makes `.find()` more permissive.
2. **Timing**: The shadow DOM content is rendered asynchronously by the Lit component. On a faster machine (Jenkins), the shadow root might be ready before Cypress queries. On a slower machine, the button might not exist yet when Cypress looks.

Either way, relying on implicit behavior is fragile. Adding `includeShadowDom: true` makes the intent explicit and works reliably on all machines and Cypress versions.

---

## The Fix

In `cypress.config.ts`:

```typescript
export default defineConfig({
  includeShadowDom: true,    // ← added: pierce into web component shadow roots
  component: {
    devServer: {
      framework: "angular",
      bundler: "webpack",
    },
    // ...
  },
});
```

This is a global setting that applies to all `cy.get()`, `.find()`, `.contains()` etc. It tells Cypress: "whenever you search for elements, also look inside shadow DOM."

---

## Summary

| Concept | Explanation |
|---------|-------------|
| **Light DOM** | Normal HTML tree. Angular components live here. Cypress searches here by default. |
| **Shadow DOM** | Hidden HTML tree inside a web component. Invisible to normal DOM queries. |
| **Lit** | Framework used by Signal Iduna Design System v5 to build web components with shadow DOM. |
| **`<slot>`** | A window that projects light DOM content into shadow DOM visually (but not structurally). |
| **`includeShadowDom`** | Cypress config that tells it to also search inside shadow roots when finding elements. |
| **Why it broke** | The sort `<button>` lives in `si-table-cell`'s shadow DOM. Without the config, Cypress can't find it. |
