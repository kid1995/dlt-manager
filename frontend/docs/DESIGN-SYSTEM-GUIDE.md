# Signal Iduna Design System Guide (v3 → v5)

## Architecture

The design system is a monorepo with three main packages:

```
@signal-iduna/ui           → Core web components (Lit 3, ES modules)
@signal-iduna/ui-angular   → Angular proxy components (standalone)
@signal-iduna/ui-migrator  → Automated migration CLI tool
```

### How It Works

```
┌─────────────────────────────────────────────────────┐
│  @signal-iduna/ui (Lit Web Components)              │
│  - 98 components, self-registering via customElements│
│  - CSS custom properties for theming                 │
│  - Slots for content projection                      │
│  - Shadow DOM for style encapsulation                │
└──────────────────────┬──────────────────────────────┘
                       │ dependency
┌──────────────────────▼──────────────────────────────┐
│  @signal-iduna/ui-angular (Angular Proxies)          │
│  - 70+ standalone components                         │
│  - Maps Angular @Input() to web component properties │
│  - Maps Angular @Output() to custom events           │
│  - <ng-content> passes through to <slot>             │
└──────────────────────┬──────────────────────────────┘
                       │ imported by
┌──────────────────────▼──────────────────────────────┐
│  Your Angular Application                            │
│  - Import individual components: SiButtonNg, etc.    │
│  - Use si-* elements in templates                    │
│  - Override styles via CSS custom properties          │
└─────────────────────────────────────────────────────┘
```

---

## v3 vs v5: What Changed

### Integration Model

| | v3 | v5 |
|---|---|---|
| **Package** | `@signal-iduna/ui-angular-proxy` | `@signal-iduna/ui-angular` |
| **Import style** | `SignalIdunaUiModule` (one module) | Individual: `SiButtonNg`, `SiHeadingNg`, etc. |
| **Schema** | `CUSTOM_ELEMENTS_SCHEMA` required | Not needed — proxies are typed Angular components |
| **Component style** | NgModule-based | Standalone components (`standalone: true`) |
| **Web component registration** | `import '@signal-iduna/ui'` in each component | One import in `main.ts` (proxies pull it in) |
| **Change detection** | Default | `OnPush` with `detach()` for performance |

### v3 Code

```typescript
import '@signal-iduna/ui'
import { SignalIdunaUiModule } from '@signal-iduna/ui-angular-proxy'

@NgModule({
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  imports: [SignalIdunaUiModule],
  declarations: [MyComponent]
})
export class MyModule {}
```

### v5 Code

```typescript
import { SiButtonNg, SiHeadingNg, SiIconNg } from '@signal-iduna/ui-angular'

@Component({
  standalone: true,
  imports: [SiButtonNg, SiHeadingNg, SiIconNg],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyComponent {}
```

### Component & Attribute Renames

| v3 | v5 |
|---|---|
| `si-app-header` | `si-header` |
| `si-header-link` | `si-header-menu-item` |
| `si-button-icon` | `si-icon` (with `si-svg` + `si-sprite`) |
| `si-key-value-multitype` | `si-key-value` |
| `applicationName` | `si-title` |
| `theme="primary"` | `si-variant="primary"` |
| `glyph="..."` | `si-svg="..."` |
| `collection="..."` | `si-sprite="..."` |
| `heading="..."` (table) | `si-title="..."` |
| `title="..."` (expander) | `si-summary="..."` |
| `[keyPosition]="'aside'"` | `si-alignment="horizontal"` |
| `[isHeader]="true"` | `[si-table-header-cell]="true"` |
| `[sortKey]` | `[si-sort-key]` |
| `[sortOrder]` | `[si-sort-order]` |
| `width`/`height` (icon) | `si-width`/`si-height` |
| `'asc'`/`'desc'` | `'ascending'`/`'descending'` |
| `TableCellSortOrder` | `SiTableCellSortOrder` |
| `public/assets/` paths | `assets/` (removed `public/` prefix) |
| `reset.css` | **Removed in v5** |

### Structural Changes (Most Important)

v5 web components require **native HTML elements inside** for proper styling:

#### si-button

```html
<!-- v3: Direct content -->
<si-button theme="primary" (click)="doSomething()">
  Click me
</si-button>

<!-- v5: Must wrap <button> inside -->
<si-button si-variant="primary">
  <button (click)="doSomething()">
    <span>Click me</span>
  </button>
</si-button>

<!-- v5: Icon-only button -->
<si-button si-variant="ghost" [si-icon-only]="true">
  <button title="Refresh" (click)="refresh()">
    <si-icon si-svg="functionalSync" si-sprite="navigation"></si-icon>
  </button>
</si-button>
```

#### si-header

```html
<!-- v3 -->
<si-app-header applicationName="My App">
  <si-header-link>
    <si-button-icon glyph="functionalLogin" collection="essentials"></si-button-icon>
    <span slot="label">Logout</span>
  </si-header-link>
</si-app-header>

<!-- v5: Needs si-header-menu > ul > li > si-header-menu-item > a -->
<si-header si-title="My App">
  <a href="/" slot="logo" aria-label="home">
    <si-header-logo></si-header-logo>
  </a>
  <si-header-menu>
    <ul>
      <li>
        <si-header-menu-item si-text-visibility="all">
          <a href="javascript:void(0)" (click)="logout()">
            <si-icon si-svg="functionalLogin" si-sprite="essentials"
                     si-width="24px" si-height="24px"></si-icon>
            Logout
          </a>
        </si-header-menu-item>
      </li>
    </ul>
  </si-header-menu>
</si-header>
```

### Loading Web Components (ES Module)

```typescript
// v3: In angular.json scripts array (loaded as classic <script>)
"scripts": ["node_modules/@signal-iduna/ui/public/index.js"]

// v5: In main.ts as ES module import (bundled by Vite)
import '@signal-iduna/ui'
// angular.json: "scripts": []
```

**Why:** v5 `index.js` uses `export` statements (ES module). The `scripts` array loads files as `<script defer>` (classic), which causes `SyntaxError: Unexpected token 'export'`.

---

## How the Proxy Pattern Works

Each Angular proxy wraps a Lit web component:

```typescript
// button.proxy.ts (simplified)
@ProxyCmp({ inputs: ['siVariant', 'siIconAlign', 'siIconOnly', 'siSize'] })
@Component({
  selector: 'si-button,si-button-v5',    // Both tags work
  template: '<ng-content></ng-content>',  // Transparent pass-through
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class SiButtonNg {
  @Input({ alias: 'si-variant' }) siVariant?: string;
  @Input({ alias: 'si-icon-align' }) siIconAlign?: string;
  @Input({ alias: 'si-icon-only' }) siIconOnly?: boolean;
  @Input({ alias: 'si-size' }) siSize?: number;

  constructor() {
    this.c.detach();  // OnPush optimization
    this.el = this.r.nativeElement;
  }
}
```

**@ProxyCmp** creates getters/setters that proxy Angular `@Input()` values to the underlying web component's properties:

```typescript
// proxy.utils.ts (simplified)
Object.defineProperty(Cmp.prototype, 'siVariant', {
  get() { return this.el['siVariant']; },
  set(val) { this.el['siVariant'] = val; }
});
```

---

## CSS Custom Properties (Theming)

### Global Tokens

The design system exposes CSS custom properties for theming. Include in `angular.json`:

```json
"styles": [
  "node_modules/@signal-iduna/ui/assets/styles/tokens.css",
  "node_modules/@signal-iduna/ui/assets/styles/font.css"
]
```

Available token categories:

| Category | Examples |
|----------|---------|
| **Colors** | `--color-primary-100: #1a3691`, `--color-secondary-100: #49d8d7` |
| **Spacing** | `--space-50: 0.5rem`, `--space-100: 1rem`, `--space-200: 2rem` |
| **Radius** | `--radius-100: 4px`, `--radius-200: 8px` |
| **Shadows** | `--shadow-50`, `--shadow-100` |
| **Duration** | `--duration-25: 0.15s`, `--duration-50: 0.3s` |
| **Typography** | `--copy-100: 1rem`, `--heading-100: 1rem` |

### Overriding Component Styles

Each component exposes `--si-*` CSS custom properties. Override them on the component selector or a parent:

#### Heading

```css
si-heading {
  --si-heading-color: var(--color-primary-100);  /* Text color */
  --si-heading-hyphens: break-word;               /* Word breaking */
}
```

#### Icon

```css
si-icon {
  --si-icon-fill: var(--color-secondary-100);  /* Icon color */
  --si-icon-width: 32px;                        /* Icon size */
  --si-icon-height: 32px;
  --si-icon-transition: 0.3s;                   /* Fill transition */
}
```

#### Button

```css
si-button {
  --si-button-align: flex-start;  /* Text/icon alignment */
}
```

#### Scoped Overrides

```css
/* Only headings inside the login form */
.login-form-container > si-heading {
  --si-heading-color: var(--color-primary-100);
  margin-bottom: 1.5em;
}

/* Error state icon */
.error si-icon {
  --si-icon-fill: var(--color-error-100);
}
```

### Finding Available CSS Properties

Each component documents its CSS custom properties via `@cssprop` JSDoc tags in the source. Check:
1. Storybook docs at http://localhost:4201
2. Component source files in `libs/ui/src/lib/components/*/`
3. The `custom-elements.json` manifest

---

## Icon System

### How It Works

`si-icon` renders an SVG `<use>` element referencing a sprite file:

```html
<si-icon si-svg="functionalCheckmark" si-sprite="essentials"></si-icon>
```

Resolves to:

```html
<svg><use href="/sprites-essentials.svg#functionalCheckmark" /></svg>
```

### Available Sprites

| Sprite | Content |
|--------|---------|
| `essentials` | Core icons (checkmark, alert, arrow, close, etc.) |
| `actions` | Action icons (edit, delete, redo, scale, etc.) |
| `navigation` | Navigation (menu, breadcrumb, sync, arrows, etc.) |
| `communication` | Mail, phone, chat, etc. |
| `finance` | Finance domain icons |
| `mobility` | Mobility domain icons |
| `misc` | Miscellaneous |
| `illustrative` | Illustrative icons |
| `detailed` | Detailed icons |
| `flags` | Country flags |
| `logo` | Full SI logo |
| `logo-icon` | SI logo icon only |

### Configuring Sprite Path

```html
<!-- In root component or index.html -->
<si-icon-configuration si-sprite-path="/assets/icons"></si-icon-configuration>
```

Ensure `angular.json` copies sprites:

```json
"assets": [
  { "glob": "*", "input": "node_modules/@signal-iduna/ui/assets/icons/", "output": "." }
]
```

---

## Slots

v5 components use the Web Components slot system for content projection.

### Common Slot Patterns

| Component | Slots | Usage |
|-----------|-------|-------|
| `si-button` | default | `<button>`, `<si-icon>`, `<span>`, `<si-loader>` |
| `si-header` | `logo`, default | Logo link, menu items |
| `si-key-value` | `key`, `value` | `<div slot="key">`, `<div slot="value">` |
| `si-expander` | default | Direct content |
| `si-header-menu-item` | default | `<a>` with icon and text |
| `si-icon` | none | Self-contained (uses attributes) |

### Slot Validation

v5 components validate slotted content at runtime. If you put wrong elements inside, you'll see console warnings like:

```
[si-button] Expected <button> as slotted child, got <div>
```

---

## Migration Tooling

The `@signal-iduna/ui-migrator` automates most of the v3 → v5 migration:

```bash
npx ui-migrator --source /path/to/project --target v5
```

Coverage: 74 components, 69% fully auto-migratable, 89% partially auto-migratable.

**What it does:**
1. Renames component tags and attributes in HTML templates
2. Updates TypeScript imports from `ui-angular-proxy` to `ui-angular`
3. Adds individual component imports to `@Component.imports`
4. Flags manual migration TODOs as HTML comments

**What it does NOT do:**
- Add inner `<button>` elements inside `si-button`
- Restructure `si-header-menu` with `ul/li/a`
- Move `angular.json` scripts to `main.ts`
- Fix Sass `@import` → `@use`
- Update Cypress tests for Shadow DOM

---

## Most Important Things to Care About

### 1. Every `si-button` Needs an Inner `<button>`

This is the #1 visual breakage. Without it, buttons render as unstyled text. Move `(click)` to the inner `<button>`.

### 2. Boolean Attributes Need Property Binding

```html
<!-- WRONG: gives empty string "" -->
<si-button si-icon-only>

<!-- CORRECT: gives boolean true -->
<si-button [si-icon-only]="true">
```

### 3. Shadow DOM Changes Everything for Testing

- Cypress needs `includeShadowDom: true`
- Click targets may be inside shadow DOM: use `.find('button').click()`
- Custom events bubble with `composed: true` but `$event` is `CustomEvent`, not the typed body

### 4. `$event` in Sort Events is `CustomEvent` at Runtime

Angular types say `$event: TableCellSortEventBody<T>`, but at runtime it's `CustomEvent<TableCellSortEventBody<T>>`. Access data via `$event.detail.sortKey`. This compiles fine but silently does nothing if you use `$event.sortKey`.

### 5. Don't Wrap `si-header` in Layout Divs

The web component manages its own full-width layout. Wrapping elements break positioning.

### 6. ES Module Loading

`@signal-iduna/ui/index.js` is ESM. Load it as `import '@signal-iduna/ui'` in `main.ts`, not in `angular.json` scripts.

### 7. CSS Override Scope

CSS custom properties respect Shadow DOM boundaries. Override on the component selector or a parent — not on internal shadow DOM elements (you can't reach them).
