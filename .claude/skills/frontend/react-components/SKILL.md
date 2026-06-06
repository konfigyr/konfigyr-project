---
name: react-components
description: Component conventions including named exports, TypeScript interfaces, data fetching via hooks, styling with Tailwind, and using CVA for variants. Use when building new components or updating existing UI.
---

# React Components

## Component Convention

```typescript
// components/ui/button.tsx

// 1. Props interface
interface ButtonProps extends React.ComponentPropsWithoutRef<'button'> {
  variant?: 'default' | 'outline' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
}

// 2. Named export (never export default)
export function Button({
  variant = 'default',
  size = 'md',
  className,
  ...props
}: ButtonProps) {
  return (
    <button
      data-slot="button"
      className={cn(
        'inline-flex items-center justify-center rounded font-medium',
        variant === 'default' && 'bg-primary text-primary-fg',
        variant === 'outline' && 'border border-input',
        size === 'sm' && 'h-8 px-2 text-sm',
        size === 'md' && 'h-10 px-3',
        size === 'lg' && 'h-12 px-4',
        className,
      )}
      {...props}
    />
  )
}
```

## Using CVA (Class Variance Authority)

```typescript
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from './utils'

const cardVariants = cva('rounded-lg border', {
  variants: {
    variant: {
      default: 'bg-card text-card-foreground',
      elevated: 'shadow-lg',
    },
    padding: {
      sm: 'p-3',
      md: 'p-6',
      lg: 'p-8',
    },
  },
  defaultVariants: {
    variant: 'default',
    padding: 'md',
  },
})

interface CardProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardVariants> {}

export function Card({
  variant,
  padding,
  className,
  ...props
}: CardProps) {
  return (
    <div
      className={cn(cardVariants({ variant, padding }), className)}
      {...props}
    />
  )
}
```

## Data Fetching in Components

```typescript
// ✓ Correct: Use hooks for data
function NamespaceCard({ slug }: { slug: string }) {
  const { data: namespace } = useGetNamespace(slug)
  return <div>{namespace?.name}</div>
}

// ✗ Wrong: Direct API call
function NamespaceCard({ slug }) {
  const [data, setData] = useState(null)
  useEffect(() => {
    // Never do this - use hooks instead
    fetch(`/api/namespaces/${slug}`).then(r => r.json()).then(setData)
  }, [slug])
}
```

## Styling Rules

- Use Tailwind utilities
- Use CSS custom properties for colors (`var(--primary)`)
- Support dark mode with `dark:` prefix
- Use `cn()` for conditional class merging

```typescript
export function Component() {
  return (
    <div className={cn(
      // Base styles
      'flex items-center justify-center p-4',
      // Conditional
      isActive && 'bg-primary text-white',
      // Dark mode
      'dark:bg-slate-900 dark:text-slate-50',
      // Responsive
      'sm:p-2 lg:p-6',
    )}>
      Content
    </div>
  )
}
```

## Component Organization

```
components/
├── ui/                    # Reusable primitives
│   ├── button.tsx
│   ├── card.tsx
│   ├── dialog.tsx
│   └── utils.ts          # cn(), styling helpers
├── namespace/            # Feature-specific
│   ├── NamespaceCard.tsx
│   ├── NamespaceForm.tsx
│   └── NamespaceList.tsx
├── vault/
├── layout/
└── error/
```

## TypeScript Props

```typescript
// With optional props
interface FormProps {
  onSubmit: (data: Data) => void | Promise<void>
  loading?: boolean
  error?: Error | null
}

// With children
interface LayoutProps {
  children: React.ReactNode
  title: string
}

// HTML attributes spread
interface InputProps
  extends React.ComponentPropsWithoutRef<'input'> {
  label?: string
}

export function Input({ label, ...props }: InputProps) {
  return (
    <div>
      {label && <label>{label}</label>}
      <input {...props} />
    </div>
  )
}
```

## data-slot Attribute

Use `data-slot` for styling hooks:

```typescript
function Card() {
  return (
    <div data-slot="card" className="border rounded p-4">
      <h2 data-slot="card-title" className="font-bold">
        Title
      </h2>
    </div>
  )
}

// In global CSS
[data-slot='card'] { @apply shadow; }
[data-slot='card-title'] { @apply text-lg; }
```

## Verification Checklist

- [ ] Named exports only (no default exports)
- [ ] Props interface defined above component
- [ ] All data fetching via hooks
- [ ] Styling uses Tailwind + CSS variables
- [ ] Dark mode supported (`dark:` classes)
- [ ] Component is testable (no hardcoded dependencies)
- [ ] Props TypeScript typed
- [ ] Responsive design considered
- [ ] Accessible (semantic HTML, ARIA attributes)

