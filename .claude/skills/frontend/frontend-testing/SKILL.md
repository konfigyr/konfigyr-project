---
name: frontend-testing
description: Testing React components and hooks with @testing-library, MSW for API mocking, test utilities, and Arrange-Act-Assert pattern. Use when writing tests for components, hooks, or routes.
---

# Frontend Testing

## Test Structure

Tests mirror `src/` structure under `test/`:

```
src/hooks/namespace/query.ts → test/hooks/namespace/query.test.ts
src/components/ui/button.tsx → test/components/ui/button.test.tsx
```

## MSW (Mock Service Worker)

All HTTP calls intercepted by MSW—no real API calls in tests.

```typescript
// test/msw/handlers.ts
import { http, HttpResponse } from 'msw'

export const handlers = [
  http.get('/api/namespaces/:slug', ({ params }) => {
    return HttpResponse.json({
      id: '1',
      slug: params.slug,
      name: 'Test Namespace',
    })
  }),

  http.post('/api/namespaces', async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json(
      { id: '2', ...body },
      { status: 201 },
    )
  }),

  http.get('/api/namespaces/:slug', () => {
    return HttpResponse.error()  // Simulate 500
  }),
]

// test/setup.ts
import { setupServer } from 'msw/node'
beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

## Testing Hooks

```typescript
// test/hooks/namespace/query.test.ts
import { renderHook, waitFor } from '@testing-library/react'
import { createWrapper } from '@konfigyr/test/utils'
import { getNamespaceQuery } from '@konfigyr/hooks/namespace/query'

test('should fetch namespace by slug', async () => {
  // Arrange
  const { result } = renderHook(
    () => useSuspenseQuery(getNamespaceQuery('test')),
    { wrapper: createWrapper() },
  )

  // Act & Assert
  await waitFor(() => {
    expect(result.current.data?.slug).toBe('test')
  })
})

test('should handle error', async () => {
  // Arrange
  server.use(
    http.get('/api/namespaces/*', () =>
      HttpResponse.error(),
    ),
  )

  // Act & Assert
  const { result } = renderHook(
    () => useSuspenseQuery(getNamespaceQuery('test')),
    { wrapper: createWrapper() },
  )

  await waitFor(() => {
    expect(result.current.error).toBeDefined()
  })
})
```

## Testing Components

```typescript
// test/components/ui/button.test.tsx
import { render, screen } from '@testing-library/react'
import { Button } from '@konfigyr/ui/button'

test('should render button with text', () => {
  render(<Button>Click me</Button>)
  expect(screen.getByText('Click me')).toBeInTheDocument()
})

test('should call onClick handler', async () => {
  const handleClick = vi.fn()
  const { user } = render(
    <Button onClick={handleClick}>Submit</Button>
  )

  await user.click(screen.getByRole('button'))
  expect(handleClick).toHaveBeenCalled()
})

test('should render variant styles', () => {
  render(<Button variant="outline">Outlined</Button>)
  const button = screen.getByRole('button')
  expect(button).toHaveClass('border', 'border-input')
})
```

## Testing Routes

```typescript
// test/routes/namespace.test.tsx
import { render, screen } from '@testing-library/react'
import { createRouter } from '@konfigyr/test/router'

test('should render namespace page', async () => {
  // Arrange & Act
  render(createRouter({ path: '/namespace/test' }))

  // Assert
  expect(await screen.findByText('Test Namespace')).toBeInTheDocument()
})

test('should show error when not found', async () => {
  server.use(
    http.get('/api/namespaces/missing', () =>
      HttpResponse.json({ error: 'Not found' }, { status: 404 }),
    ),
  )

  render(createRouter({ path: '/namespace/missing' }))
  expect(await screen.findByText(/not found/i)).toBeInTheDocument()
})
```

## Test Utilities

```typescript
// test/utils.ts
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@konfigyr/lib/query'

export function createWrapper() {
  return ({ children }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )
}

export function createRouter(options: { path: string }) {
  return <RouterProvider router={createTestRouter(options.path)} />
}
```

## Arrange-Act-Assert Pattern

```typescript
test('should update namespace name', async () => {
  // ===== ARRANGE =====
  const handleClick = vi.fn()
  render(<Form onSubmit={handleClick} />)

  // ===== ACT =====
  const input = screen.getByLabelText('Name')
  await user.type(input, 'New Name')
  await user.click(screen.getByRole('button', { name: 'Submit' }))

  // ===== ASSERT =====
  await waitFor(() => {
    expect(handleClick).toHaveBeenCalledWith({
      name: 'New Name',
    })
  })
})
```

## Testing with Server Errors

```typescript
test('should show error message on API failure', async () => {
  server.use(
    http.post('/api/namespaces', () => {
      return HttpResponse.json(
        { error: 'Validation failed' },
        { status: 400 },
      )
    }),
  )

  render(<CreateNamespaceForm />)
  await user.click(screen.getByRole('button'))

  expect(await screen.findByText('Validation failed')).toBeInTheDocument()
})
```

## Run Tests

```bash
npm run test              # Watch mode
npm run test:coverage    # Coverage report
npm run test:ci         # CI mode (once, with lint + type-check)
```

## Verification Checklist

- [ ] All tests use Arrange-Act-Assert
- [ ] No real API calls (MSW intercepts all)
- [ ] Components tested with and without data
- [ ] Error states tested
- [ ] Async operations awaited properly
- [ ] User interactions use `userEvent`
- [ ] Query results awaited with `waitFor`
- [ ] Test names describe behavior
- [ ] No hardcoded test data (use factories)
- [ ] Coverage > 80%

