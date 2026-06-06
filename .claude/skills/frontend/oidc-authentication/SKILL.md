---
name: oidc-authentication
description: OIDC authentication flow, session management via encrypted cookies, token lifecycle, useAccount hook, and accessing user data in components. Use when implementing auth features or accessing authenticated user context.
---

# OIDC Authentication

## Authentication Flow

1. Frontend redirects to Identity Provider (AuthCode + PKCE)
2. IdP authenticates user
3. IdP redirects back to `/auth/code?code=...`
4. Server exchanges code for tokens
5. Tokens stored in encrypted HTTP-only cookie
6. Token automatically included in all `/api/*` requests

## Session Management

Handled entirely in `src/middleware/authentication.ts` and `src/lib/authentication.ts`.

**Do not add auth logic in components.** Access user via `useAccount()` hook only.

```typescript
// Middleware runs on every request (server-side)
// - Checks session cookie
// - Refreshes expired tokens
// - Redirects to IdP if missing

// Tokens never exposed to client code
// - Only server-side proxy sees them
// - Components never access `localStorage` or `sessionStorage`
```

## useAccount Hook

Access current user in any component inside `AccountProvider`:

```typescript
import { useAccount } from '@konfigyr/components/account'

export function NamespaceCard() {
  const account = useAccount()  // Throws if outside provider
  
  return <div>Welcome, {account.email}</div>
}
```

**AccountProvider** wraps authenticated routes (in `_authenticated/route.tsx`):

```typescript
export const Route = createFileRoute('/_authenticated')({
  component: AuthenticatedLayout,
})

function AuthenticatedLayout() {
  return (
    <AccountProvider>
      <Outlet />
    </AccountProvider>
  )
}
```

## Environment Variables

Required for OIDC setup:

```bash
# IdP base URL
KONFIGYR_ID_URL=http://localhost:8081

# OAuth2 client credentials
KONFIGYR_OAUTH_CLIENT_ID=konfigyr-frontend
KONFIGYR_OAUTH_CLIENT_SECRET=...

# Session encryption
KONFIGYR_SESSION_KEY=... (32-byte base64)

# Backend API (server-side proxy)
KONFIGYR_API_URL=http://localhost:8080
```

## Token Access

**Never access tokens directly in components:**

```typescript
// ✗ Wrong: Tokens aren't in window or localStorage
const token = localStorage.getItem('access_token')  // undefined

// ✓ Correct: Tokens in encrypted cookie, automatically attached
// All /api/* requests include token in Authorization header
const { data } = useSuspenseQuery(getNamespaceQuery(slug))
```

## Handling 401/403

If token expires or scopes invalid:

```typescript
// ✗ Wrong: Manual error checking
if (response.status === 401) {
  localStorage.removeItem('token')
  window.location.href = '/login'
}

// ✓ Correct: Middleware redirects automatically
// If token invalid, request to IdP
// If no session, redirect to AuthCode flow
```

## OAuth2 Scopes in Frontend

Scopes determine what API endpoints user can access:

```typescript
// When requesting data, backend validates scope
// 403 Forbidden → user lacks required scope
// 401 Unauthorized → token expired or missing

try {
  const { data } = useSuspenseQuery(getNamespaceQuery(slug))
} catch (error) {
  if (error.status === 403) {
    return <div>Access denied. Contact admin.</div>
  }
}
```

## Account Data Structure

```typescript
interface Account {
  id: string
  email: string
  firstName?: string
  lastName?: string
  avatarUrl?: string
  createdAt: Date
}

// Access in component
const account = useAccount()
account.email  // user@example.com
account.firstName  // John
```

## Logout

Logout endpoint clears session:

```typescript
const logout = async () => {
  await fetch('/api/auth/logout', { method: 'POST' })
  window.location.href = '/'  // Redirect to login
}
```

## Token Refresh

Automatic in middleware (server-side). No client-side token management needed.

```
Request → Middleware checks expiration
         → If expired, refresh automatically
         → Include refreshed token in request
         → Proceed
```

## Testing Authentication

```typescript
import { render } from '@testing-library/react'
import { createRouter } from '@konfigyr/test/router'

test('should show account email', () => {
  render(createRouter({ 
    path: '/_authenticated',
    authenticated: true,  // Mock authenticated state
  }))
  
  expect(screen.getByText(/user@example.com/)).toBeInTheDocument()
})

test('should redirect to login when not authenticated', () => {
  render(createRouter({
    path: '/_authenticated',
    authenticated: false,
  }))
  
  expect(window.location.pathname).toBe('/auth/login')
})
```

## Verification Checklist

- [ ] useAccount() only used inside AccountProvider
- [ ] No token access in client code
- [ ] No localStorage/sessionStorage usage
- [ ] Environment variables configured
- [ ] Logout clears session
- [ ] 401/403 responses handled
- [ ] Components test with/without auth
- [ ] No hardcoded user data

