---
name: form-handling
description: TanStack Form setup, form validation with schemas, submission handling, error display, and loading states. Use when building forms for data entry or complex user interactions.
---

# Form Handling

## Basic Form

```typescript
import { useForm } from '@tanstack/react-form'
import { zodValidator } from '@tanstack/zod-form-adapter'
import { z } from 'zod'

// Define schema
const createNamespaceSchema = z.object({
  slug: z.string().min(1, 'Slug required').regex(/^[a-z0-9-]+$/),
  name: z.string().min(1, 'Name required').max(255),
  description: z.string().optional(),
})

type CreateNamespaceInput = z.infer<typeof createNamespaceSchema>

export function CreateNamespaceForm() {
  const { mutate, isPending } = useCreateNamespace()

  const form = useForm({
    defaultValues: {
      slug: '',
      name: '',
      description: '',
    },
    onSubmit: async ({ value }) => {
      mutate(value)
    },
    validatorAdapter: zodValidator(),
  })

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        form.handleSubmit()
      }}
    >
      <form.Field name="slug">
        {(field) => (
          <div>
            <label>Slug</label>
            <input
              value={field.state.value}
              onBlur={field.handleBlur}
              onChange={(e) => field.handleChange(e.target.value)}
            />
            {field.state.meta.errors && (
              <span className="text-destructive">
                {field.state.meta.errors[0]}
              </span>
            )}
          </div>
        )}
      </form.Field>

      <form.Field name="name">
        {(field) => (
          <div>
            <label>Name</label>
            <input
              value={field.state.value}
              onBlur={field.handleBlur}
              onChange={(e) => field.handleChange(e.target.value)}
            />
            {field.state.meta.errors && (
              <span className="text-destructive">
                {field.state.meta.errors[0]}
              </span>
            )}
          </div>
        )}
      </form.Field>

      <button type="submit" disabled={isPending}>
        {isPending ? 'Creating...' : 'Create'}
      </button>
    </form>
  )
}
```

## Field Component Wrapper

```typescript
interface FieldProps {
  name: string
  label: string
  type?: string
  required?: boolean
}

export function TextField({ name, label, type = 'text', required }: FieldProps) {
  return (
    <form.Field name={name}>
      {(field) => (
        <div className="mb-4">
          <label htmlFor={name} className="block font-medium">
            {label} {required && <span className="text-destructive">*</span>}
          </label>
          <input
            id={name}
            type={type}
            value={field.state.value}
            onBlur={field.handleBlur}
            onChange={(e) => field.handleChange(e.target.value)}
            className={cn(
              'border rounded px-3 py-2 w-full',
              field.state.meta.errors && 'border-destructive',
            )}
          />
          {field.state.meta.errors && (
            <p className="text-sm text-destructive mt-1">
              {field.state.meta.errors[0]}
            </p>
          )}
        </div>
      )}
    </form.Field>
  )
}

// Usage
<TextField name="slug" label="Slug" required />
```

## Validation

```typescript
// Zod schema
const schema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(8, 'Minimum 8 characters'),
  confirmPassword: z.string(),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

// Custom validation
const form = useForm({
  validators: {
    onChange: schema,  // Real-time validation
    onSubmit: schema,  // Submit-time validation
  },
})
```

## Async Validation

```typescript
const schema = z.object({
  slug: z.string()
    .min(1)
    .superRefine(async (val, ctx) => {
      const exists = await checkSlugExists(val)
      if (exists) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: 'Slug already taken',
        })
      }
    }),
})
```

## Form Handler Integration

```typescript
// routes/namespace/index-handler.ts
export const updateNamespaceAction = async ({ request, params }) => {
  const formData = await request.formData()
  const name = formData.get('name')

  try {
    const response = await fetch(
      `/api/namespaces/${params.namespace}`,
      {
        method: 'PATCH',
        body: JSON.stringify({ name }),
      },
    )

    if (!response.ok) {
      return json({ error: 'Update failed' }, { status: 400 })
    }

    return json({ success: true })
  } catch (error) {
    return json({ error: error.message }, { status: 500 })
  }
}

// routes/namespace/index.tsx
import { Form } from '@tanstack/react-router'

function NamespacePage() {
  return (
    <Form action={updateNamespaceAction} method="post">
      <input type="text" name="name" />
      <button type="submit">Update</button>
    </Form>
  )
}
```

## Loading States

```typescript
<form onSubmit={handleSubmit}>
  <input
    disabled={isPending}
    placeholder="Enter namespace"
  />
  
  <button type="submit" disabled={isPending}>
    {isPending ? (
      <>
        <Spinner className="mr-2" />
        Creating...
      </>
    ) : (
      'Create'
    )}
  </button>
</form>
```

## Error Display

```typescript
function FormError({ error }: { error?: string }) {
  return error ? (
    <div className="bg-destructive/10 border border-destructive text-destructive p-3 rounded">
      {error}
    </div>
  ) : null
}

// Usage
{form.state.meta.errors && (
  <FormError error={form.state.meta.errors[0]?.message} />
)}
```

## Verification Checklist

- [ ] Schema validation defined (Zod/Yup)
- [ ] Real-time validation on change
- [ ] Form submission handling
- [ ] Error messages displayed
- [ ] Loading state during submit
- [ ] Disabled input while pending
- [ ] Success feedback (toast/redirect)
- [ ] Async validation when needed
- [ ] Form reset after success
- [ ] Accessibility (labels, error announcements)
