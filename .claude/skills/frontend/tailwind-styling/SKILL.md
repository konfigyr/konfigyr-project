---
name: tailwind-styling
description: Tailwind CSS v4 conventions - CSS-first theme config via @theme, design tokens as CSS custom properties, @source for monorepo package scanning, dark mode, and styling best practices. Use when styling components, updating design tokens, or implementing dark mode.
---

# Tailwind Styling

This project is on **Tailwind CSS v4** — CSS-first config, no `tailwind.config.ts`. Each app (`apps/console`, `apps/website`) has a single `src/styles.css` entry point; the shared design system lives in `packages/ui`.

## Entry Stylesheet

```css
/* apps/console/src/styles.css */
@import "tailwindcss";
@import "tw-animate-css";
@import "@fontsource-variable/inter";
@import "@fontsource-variable/rubik";

@import "@konfigyr/ui/tailwind.css";
@import "@konfigyr/ui/theme.css";

/* app-specific tokens/overrides go after the shared imports */
```

`@import "tailwindcss"` replaces the old `@tailwind base/components/utilities` directives. There is no `tailwind.config.ts` — theme values are registered directly in CSS via `@theme`.

## Design Tokens

Semantic tokens are raw HSL triplets in `:root`/`.dark`, then mapped into Tailwind's color namespace with `@theme inline` so they become real utilities (`bg-primary`, `text-muted-foreground`, ...):

```css
@custom-variant dark (&:is(.dark *));

@theme inline {
  --color-background: hsl(var(--background));
  --color-foreground: hsl(var(--foreground));
  --color-primary: hsl(var(--primary));
  --color-primary-foreground: hsl(var(--primary-foreground));
  --color-border: hsl(var(--border));
  --radius-md: calc(var(--radius) - 4px);
}

:root {
  --background: 0 0% 100%;
  --foreground: 212 30% 12%;
  --primary: 208 83% 42%;
  --primary-foreground: 0 0% 100%;
  --border: 210 4% 89%;
  --radius: 12px;
}

.dark {
  --background: 0 0% 0%;
  --foreground: 212 4% 95%;
  --border: 212 7% 39%;
}

@layer base {
  * {
    @apply border-border outline-ring/50;
  }

  body {
    @apply font-sans antialiased bg-background text-foreground;
  }
}
```

Multiple `@theme` blocks across imported files merge into one global theme — this is how the shared `packages/ui/src/theme.css` and an app's own `styles.css` can each contribute `@theme inline` entries (e.g. console adds `--color-sidebar`, `--color-chart-1` on top of the shared theme).

## Using Design Tokens

```typescript
// ✓ Correct: use the token utility
<div className="bg-primary text-primary-foreground" />

// ✗ Wrong: hardcoded color
<div className="bg-blue-500 text-white" />

// ✓ Reference a raw CSS variable directly when there's no registered utility
// (e.g. interaction-state tokens like --btn-primary-hover-bg)
<button className="bg-primary hover:bg-(--btn-primary-hover-bg)" />

// ✓ Dark mode via the custom variant
<div className="bg-primary dark:bg-secondary" />
```

## Monorepo: Sharing Theme & Sources Across Packages

`@source` paths are resolved **relative to the file that declares them**, not the importing file. So instead of an app reaching across the workspace with a relative glob (`@source "../../../packages/ui/src/**/*.{ts,tsx}";`), each package that ships Tailwind-class-bearing components owns its own `@source` declaration and exports it:

```css
/* packages/ui/src/tailwind.css */
@source "./**/*.{ts,tsx}";
```

```json
// packages/ui/package.json
"exports": {
  "./tailwind.css": "./src/tailwind.css",
  "./theme.css": "./src/theme.css"
}
```

Consuming apps just import it:

```css
@import "@konfigyr/ui/tailwind.css";
```

Do this for **every** workspace package whose components are consumed outside their own package and carry Tailwind classes (e.g. `packages/markdown-editor/src/tailwind.css`, exported and imported by `console` the same way). A package that ships components but never exports its `@source` file will silently lose generated classes for anyone consuming it outside its own directory tree.

The shared design tokens follow the same pattern: `packages/ui/src/theme.css` holds the `@custom-variant`, `@theme inline` mapping, `:root`/`.dark` palette, and base layer shared by every app. Apps `@import` it and then layer only their own additions (fonts, extra chart/sidebar tokens, etc.) after it — see `apps/console/src/styles.css` vs `apps/website/src/styles.css` for a worked example of shared-theme + app-specific-extension.

## Component Composition

```typescript
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@konfigyr/ui/lib/utils';

export const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-full text-sm font-bold',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground hover:bg-(--btn-primary-hover-bg)',
        outline: 'border-primary bg-transparent text-primary',
      },
      size: {
        default: 'h-9 px-6',
        sm: 'h-7 px-4 text-xs',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  },
);

export function Button({ className, variant, size, ...props }: ButtonProps) {
  return (
    <button
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  );
}
```

`cn()` (from `@konfigyr/ui/lib/utils`) wraps `clsx` + `tailwind-merge` — always run classes through it when a `className` prop can be overridden by a caller.

## Responsive Design

```typescript
// Mobile-first
<div className="p-3 sm:p-4 md:p-6 lg:p-8">Content</div>

// Responsive grid
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
  {items.map(item => <Card key={item.id} {...item} />)}
</div>
```

## Typography

Font tokens are plain CSS variables set in `:root`, referenced from `@theme` / base-layer rules — no `fontFamily` config block:

```css
:root {
  --font-heading: "Rubik", sans-serif;
  --font-sans: "Inter Variable", sans-serif;
  --font-mono: "JetBrains Mono", monospace; /* console only */
}

@layer base {
  body {
    @apply font-sans antialiased;
  }

  h1, h2, h3, h4, h5, h6, .font-heading {
    font-family: var(--font-heading), sans-serif;
  }
}
```

## Focus & Keyboard Navigation

```typescript
// ✓ Good: visible focus
<button className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2" />

// ✗ Bad: no focus styling
<button className="px-3 py-2 rounded" />
```

## Verification Checklist

- [ ] All colors use design tokens (`bg-primary`, not `bg-blue-500`)
- [ ] Dark mode variants present where the app supports dark mode
- [ ] New workspace package with Tailwind-class components exports its own `@source` file
- [ ] Responsive design tested (`sm`, `md`, `lg` breakpoints)
- [ ] Focus states visible for keyboard navigation
- [ ] `cn()` used wherever a component accepts `className`
- [ ] Contrast ratios meet WCAG AA
