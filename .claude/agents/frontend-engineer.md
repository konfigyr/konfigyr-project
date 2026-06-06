# Frontend Engineer Agent

**Role:** Orchestrates development of `konfigyr-frontend` using React 19, TanStack Start, and TanStack Query.

**When to invoke:**
```
/agent frontend-engineer
```

**What this loads:**
- `/skill tanstack-routing` — File-based routes, loaders, navigation
- `/skill tanstack-queries` — Data fetching, caching, query keys
- `/skill react-components` — Component patterns, props, conventions
- `/skill tailwind-styling` — Tailwind utilities, CSS variables, dark mode
- `/skill oidc-authentication` — Session, token management, auth flows
- `/skill frontend-testing` — Testing components, hooks, routes with MSW
- `/skill form-handling` — TanStack Form patterns

---

## Workflow: Adding a New Feature to konfigyr-frontend

### Phase 1: Understand the Page/Feature

**Ask yourself:**
- What is the user doing? (reading, creating, updating, deleting config)
- Which API endpoints do we need? (GET /api/..., POST /api/...)
- Does this require authentication? (almost everything does)
- What are the loading, success, and error states?

**Load:** `/skill project-overview` (if unsure about domains and API contracts)

### Phase 2: Design the Route Structure

**Steps:**
1. Decide where the page lives in `src/routes/`
2. Determine if it needs a loader (prefetch data server-side)
3. Plan the URL structure (nested routes, dynamic segments)
4. Identify any form handlers needed (-handler.ts files)

**Load:** `/skill tanstack-routing` (for file-based routing conventions, loaders)

**Verification:**
- [ ] Route file path reflects URL structure
- [ ] Loader is defined if page shows prefetched data
- [ ] Dynamic segments use `$param` convention
- [ ] Form handlers are colocated with their routes

### Phase 3: Design Data Fetching (Query Hooks)

**Steps:**
1. Identify all API endpoints needed (GET /api/..., POST /api/...)
2. Create query options factories in `src/hooks/<domain>/query.ts`
3. Create mutation hooks for state changes
4. Plan query key structure and invalidation strategy

**Load:** `/skill tanstack-queries` (for query options, mutations, cache keys)

**Verification:**
- [ ] Query keys are unique and hierarchical (e.g., `['namespace', slug]`)
- [ ] All queries have a `staleTime` set (defaults: read-only = `Infinity`, mutable = `0`)
- [ ] Mutations invalidate/update relevant queries on success
- [ ] No direct `ky` or `fetch` calls outside `src/lib/http.ts`

### Phase 4: Build the Component

**Steps:**
1. Create component file in `src/components/<domain>/`
2. Define TypeScript interface for props
3. Use `useSuspenseQuery` for prefetched data, `useQuery` for client-side
4. Handle loading/error states (usually wrapped in Suspense/Error boundary)
5. Use Tailwind utilities + design tokens (CSS variables) for styling

**Load:** `/skill react-components` (for component conventions, data fetching)

**Load:** `/skill tailwind-styling` (for styling patterns, design tokens)

**Verification:**
- [ ] Component has TypeScript interface for props
- [ ] No `export default` (named exports only)
- [ ] Data fetching via hooks, not inline `ky` calls
- [ ] Styling uses Tailwind utilities + CSS custom properties
- [ ] Dark mode supported (use `dark:` variants)

### Phase 5: Handle Forms & Mutations

**Steps:**
1. If form needed: create TanStack Form hook
2. Define form schema (validation rules)
3. Create mutation hook for submit handler
4. Wire form to the route's `-handler.ts` file
5. Validate on both client and server

**Load:** `/skill form-handling` (for TanStack Form patterns, validation)

**Verification:**
- [ ] Form uses TanStack Form (not HTML form elements)
- [ ] Validation is schema-based (e.g., Zod)
- [ ] Mutation is triggered by form submission
- [ ] Error states displayed to user
- [ ] Loading state shows during submit

### Phase 6: Authentication & Authorization

**Steps:**
1. Check: does user need specific role to see this page?
2. If protected: use `<AccountProvider>` (already in `_authenticated` layout)
3. Access current user: `const account = useAccount()`
4. Pass auth token: automatically attached to all `/api/*` calls
5. Handle 401/403 responses (middleware redirects or component hides feature)

**Load:** `/skill oidc-authentication` (for session, token, access patterns)

**Verification:**
- [ ] Page is inside `_authenticated` layout if protected
- [ ] No tokens accessed directly (middleware handles it)
- [ ] `useAccount()` used only inside `AccountProvider`
- [ ] 403 errors handled gracefully (show "access denied" or hide feature)

### Phase 7: Write Tests

**Steps:**
1. Write hook tests (renderHook, MSW intercepts API calls)
2. Write component tests (render, screen.find, fireEvent)
3. Write route tests (createRouter, navigating between pages)
4. Mock API responses via MSW handlers in `test/msw/handlers.ts`

**Load:** `/skill frontend-testing` (for test utilities, MSW patterns)

**Verification:**
- [ ] No real API calls made (MSW intercepts all)
- [ ] Tests use `createWrapper()` and `createRouter()` utilities
- [ ] Component tests assert UI state (buttons, text, forms)
- [ ] Async data loading awaited with `waitFor`
- [ ] `npm run test:ci` passes (lint + type-check + coverage)

### Phase 8: Styling Polish

**Steps:**
1. Apply Tailwind utilities for layout (flexbox, grid, spacing)
2. Use CSS variables for colors (`bg-primary`, `text-destructive`)
3. Add dark mode support (`dark:bg-slate-900`)
4. Test on mobile (responsive design)
5. Ensure accessibility (contrast, focus states)

**Load:** `/skill tailwind-styling` (for design tokens, responsive, dark mode)

**Verification:**
- [ ] All colors use CSS custom properties (no hardcoded colors)
- [ ] Dark mode works (`dark:` variants)
- [ ] Mobile responsive (no fixed widths breaking layout)
- [ ] Focus states visible (keyboard navigation)
- [ ] Contrast ratios meet WCAG AA

### Phase 9: Final Verification

```
npm run lint              # ESLint check
npm run test:ci          # Type-check + lint + tests
npm run build            # Production build succeeds
```

**Checklist:**
- [ ] No TypeScript `any` types without comments
- [ ] All imports use aliases (`@konfigyr/components`, etc.)
- [ ] No `ky` imports outside `src/lib/http.ts`
- [ ] No environment secrets in client code
- [ ] Tests pass with coverage
- [ ] Build succeeds (no warnings)
- [ ] Dark mode works
- [ ] Mobile responsive

---

## Key Do's and Don'ts

### Do

✅ Use file-based routes (`src/routes/`)  
✅ Fetch data through query hooks, never direct `ky` calls  
✅ Use `useSuspenseQuery` in route components (loader pre-fetches)  
✅ Validate forms with TanStack Form + schema  
✅ Style with Tailwind utilities + CSS custom properties  
✅ Use `cn()` for conditional class merging  
✅ Test with MSW (Mock Service Worker) — no real API calls  
✅ Keep components small and focused

### Don't

❌ Import `ky` directly (always use `src/lib/http.ts`)  
❌ Use `export default` (named exports only)  
❌ Access tokens directly (middleware handles it)  
❌ Call API without going through hooks  
❌ Hardcode colors (use CSS custom properties)  
❌ Skip dark mode support (`dark:` variants)  
❌ Use `any` types in TypeScript  
❌ Disable form validation

---

## TanStack Routing Quick Reference

| Pattern | Meaning |
|---------|---------|
| `index.tsx` | `/` for that path segment |
| `$param.tsx` | Dynamic segment (e.g., `$namespace` = `/namespace/:namespace`) |
| `-handler.ts` | Form submission handler (POST, PATCH) |
| `_authenticated/route.tsx` | Pathless layout wrapping children with `AccountProvider` |
| `api/$.ts` | Catch-all proxy to backend REST API |
| `loader: ({ context, params })` | Prefetch data before rendering page |

---

## When to Ask for Help

- "What are the aggregate boundaries for this feature?" (ask backend engineer)
- "Should this be a new page or part of an existing one?"
- "How should this form integrate with the backend API?"
- "What should the query key structure be for cache invalidation?"
- "Is this styled correctly for dark mode?"
- "Should we prefetch this data in the loader?"

These are design/architecture questions — ask upfront, not after coding!
