---
name: tailwind-styling
description: Tailwind CSS conventions, design tokens via CSS custom properties, dark mode support, responsive design, and styling best practices. Use when styling components, updating design tokens, or implementing dark mode.
---

# Tailwind Styling

## Design Tokens (CSS Variables)

Define all colors and spacing in `src/styles.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 0 0% 3%;
    --primary: 0 84% 60%;
    --primary-foreground: 0 85% 97%;
    --secondary: 217 33% 17%;
    --accent: 142 71% 45%;
    --destructive: 0 84% 60%;
    --muted: 0 0% 96%;
    --muted-foreground: 0 0% 45%;
    --border: 0 0% 89%;
    --input: 0 0% 89%;
    --ring: 0 84% 60%;
    --radius-sm: 0.25rem;
    --radius-md: 0.375rem;
    --radius-lg: 0.5rem;
  }

  .dark {
    --background: 0 0% 3%;
    --foreground: 0 0% 98%;
    --primary: 0 84% 60%;
    --secondary: 217 100% 87%;
    --muted: 0 0% 14%;
    --muted-foreground: 0 0% 63%;
    --border: 0 0% 14%;
    --input: 0 0% 20%;
  }

  * {
    @apply border-border;
  }

  body {
    @apply bg-background text-foreground;
  }
}
```

## Using Design Tokens

```typescript
// ✓ Correct: Use token name
<div className="bg-primary text-primary-foreground" />

// ✗ Wrong: Hardcoded color
<div className="bg-blue-500 text-white" />

// ✓ Correct: Dark mode
<div className="bg-primary dark:bg-secondary" />
```

## Tailwind Config

```javascript
// tailwind.config.ts
import type { Config } from 'tailwindcss'

export default {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: 'hsl(var(--primary))',
        'primary-foreground': 'hsl(var(--primary-foreground))',
        secondary: 'hsl(var(--secondary))',
        accent: 'hsl(var(--accent))',
        muted: 'hsl(var(--muted))',
        'muted-foreground': 'hsl(var(--muted-foreground))',
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        destructive: 'hsl(var(--destructive))',
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
      },
    },
  },
} satisfies Config
```

## Responsive Design

```typescript
// Mobile-first approach
<div className={cn(
  // Mobile: small padding
  'p-3',
  // Tablet and up
  'sm:p-4',
  // Desktop and up
  'md:p-6',
  'lg:p-8',
)}>
  Content
</div>

// Responsive grid
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
  {items.map(item => <Card key={item.id} {...item} />)}
</div>
```

## Dark Mode

```typescript
// ✓ Correct: Explicit dark variants
<div className={cn(
  'bg-white text-black',
  'dark:bg-slate-900 dark:text-white',
)} />

// Component with dark support
export function Button() {
  return (
    <button className={cn(
      'bg-primary text-primary-foreground',
      'dark:bg-primary dark:text-primary-foreground',  // Can be same
      'hover:opacity-90',
      'dark:hover:opacity-80',
    )} />
  )
}
```

## Typography

Font stack in CSS:

```css
@layer base {
  body {
    @apply font-sans;
  }

  h1, h2, h3 {
    @apply font-heading;
  }

  code {
    @apply font-mono;
  }
}
```

Config:

```javascript
theme: {
  extend: {
    fontFamily: {
      sans: ['Geist', 'system-ui', 'sans-serif'],
      heading: ['Rubik', 'sans-serif'],
      mono: ['JetBrains Mono', 'monospace'],
    },
  },
}
```

## Focus & Keyboard Navigation

```typescript
// ✓ Good: Visible focus
<button className={cn(
  'px-3 py-2 rounded',
  'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2',
  'dark:focus:ring-offset-slate-950',
)} />

// ✗ Bad: No focus styling
<button className="px-3 py-2 rounded" />
```

## Component Composition

```typescript
// Compose with cn()
export function Card({ className, ...props }) {
  return (
    <div
      className={cn(
        'rounded-lg border bg-card p-6 text-card-foreground',
        'shadow-sm hover:shadow-md transition-shadow',
        'dark:border-slate-700',
        className,  // Allows overrides
      )}
      {...props}
    />
  )
}
```

## Verification Checklist

- [ ] All colors use design tokens (CSS variables)
- [ ] Dark mode variants present
- [ ] Responsive design tested (sm, md, lg breakpoints)
- [ ] Focus states visible for keyboard navigation
- [ ] No hardcoded colors in component code
- [ ] Font stack defined
- [ ] Spacing consistent (use Tailwind scale)
- [ ] Contrast ratios meet WCAG AA
- [ ] Print styles considered if needed

