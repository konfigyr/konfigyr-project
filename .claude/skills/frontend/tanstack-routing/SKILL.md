---
name: tanstack-routing
description: File-based routing with TanStack Router, creating routes, loaders for data prefetching, dynamic segments, and form handlers. Use when adding new pages or modifying navigation structure.
---

# TanStack Router & File-Based Routing

## Overview

TanStack Start uses file-based routing where folder structure determines URLs.

| File | URL | Purpose |
|------|-----|---------|
| `index.tsx` | `/` at that level | Page component |
| `$param.tsx` | `/:param` | Dynamic segment |
| `-handler.ts` | Form submission | POST/PATCH/DELETE handlers |
| `_layout/route.tsx` | Pathless layout | Wraps children, no URL change |

## Example Route Structure

```
routes/
├── __root.tsx                    # Root layout + providers
├── _authenticated/route.tsx      # Pathless: adds AccountProvider
│   ├── index.tsx                 # /dashboard
│   └── namespace/
│       └── $namespace/
│           ├── index.tsx         # /namespace/:namespace
│           └── index-handler.ts  # Form actions
├── auth/code.tsx                 # /auth/code (OAuth callback)
└── api/$.ts                      # /api/* (proxy)
```

## Creating a Route

```typescript
// routes/_authenticated/namespace/$namespace/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { getNamespaceQuery } from '@konfigyr/hooks/namespace/query'

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace',
)({
  // Prefetch data server-side (optional)
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(getNamespaceQuery(params.namespace)),
  
  component: NamespacePage,
})

function NamespacePage() {
  const { namespace } = Route.useParams()
  // Data already loaded by loader
  const { data } = useSuspenseQuery(getNamespaceQuery(namespace))
  
  return <div>{data.name}</div>
}
```

## Form Handlers

```typescript
// routes/namespace/$namespace/index-handler.ts
import { json } from '@tanstack/start'

export const updateNamespaceAction = async ({
  request,
  params,
}: {
  request: Request
  params: { namespace: string }
}) => {
  const formData = await request.formData()
  const name = formData.get('name') as string

  const response = await fetch(
    `/api/namespaces/${params.namespace}`,
    {
      method: 'PATCH',
      body: JSON.stringify({ name }),
    },
  )

  if (!response.ok) {
    return json({ error: 'Failed' }, { status: response.status })
  }

  return json({ success: true })
}

// Usage in component
import { Form } from '@tanstack/react-router'
import { updateNamespaceAction } from './index-handler'

<Form action={updateNamespaceAction}>
  <input name="name" />
  <button>Update</button>
</Form>
```

## Navigation

```typescript
import { Link, useNavigate } from '@tanstack/react-router'

// Link
<Link 
  to="/namespace/$namespace" 
  params={{ namespace: 'my-org' }}
>
  Go to Org
</Link>

// Programmatic
const navigate = useNavigate()
navigate({
  to: '/namespace/$namespace',
  params: { namespace: 'my-org' },
})
```

## Loaders Best Practices

- Use `ensureQueryData()` to prefetch
- Loader runs server-side before render
- Component uses `useSuspenseQuery()` (no loading state needed)
- Throw errors in loader to trigger error boundary

```typescript
export const Route = createFileRoute('/_authenticated/namespace/$namespace')({
  loader: ({ context, params }) => {
    // Prefetch data
    return context.queryClient.ensureQueryData(
      getNamespaceQuery(params.namespace)
    )
  },
  errorComponent: () => <div>Namespace not found</div>,
  component: NamespacePage,
})
```

## Pathless Layouts

Use `_layout/route.tsx` for wrappers that don't affect URL:

```typescript
// routes/_authenticated/route.tsx
import { createFileRoute, Outlet } from '@tanstack/react-router'
import { AccountProvider } from '@konfigyr/components/account'

export const Route = createFileRoute('/_authenticated')({
  component: AuthenticatedLayout,
})

function AuthenticatedLayout() {
  return (
    <AccountProvider>
      <div className="layout">
        <Navbar />
        <Outlet /> {/* Renders child routes */}
        <Footer />
      </div>
    </AccountProvider>
  )
}
```

## Verification Checklist

- [ ] Route path matches URL structure
- [ ] Loader defined if data prefetch needed
- [ ] Dynamic segments use `$param` naming
- [ ] Form handlers colocated with routes (-handler.ts)
- [ ] Components use `useSuspenseQuery` (loader prefetches)
- [ ] Error boundaries handle missing data
- [ ] Navigation uses typed `to` and `params`
- [ ] Pathless layouts use correct naming

