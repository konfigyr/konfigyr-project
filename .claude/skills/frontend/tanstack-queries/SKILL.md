---
name: tanstack-queries
description: TanStack Query patterns for data fetching, mutation hooks, query key structure, cache management, and stale time configuration. Use when working with API data, implementing cache strategies, or managing server state.
---

# TanStack Query (React Query)

## Query Hooks Pattern

All data fetching goes through hooks. Never call `ky` directly from components.

```typescript
// hooks/namespace/query.ts
import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { request } from '@konfigyr/lib/http'

// Query key constants
export const namespaceKeys = {
  all: () => ['namespaces'] as const,
  lists: () => [...namespaceKeys.all(), 'list'] as const,
  list: (filters?: Record<string, any>) => [...namespaceKeys.lists(), filters] as const,
  details: () => [...namespaceKeys.all(), 'detail'] as const,
  detail: (slug: string) => [...namespaceKeys.details(), slug] as const,
}

// Query options factory
export const getNamespaceQuery = (slug: string) =>
  queryOptions({
    queryKey: namespaceKeys.detail(slug),
    queryFn: async ({ signal }) => {
      const res = await request.get(`api/namespaces/${slug}`, { signal })
      return res.json<Namespace>()
    },
    staleTime: Infinity, // Read-only data
  })

// Hook for component use
export const useGetNamespace = (slug: string) =>
  useQuery(getNamespaceQuery(slug))

// Mutation hook
export const useUpdateNamespace = () => {
  const client = useQueryClient()
  
  return useMutation({
    mutationFn: async (def: NamespaceDefinition) => {
      const res = await request.patch(`api/namespaces/${def.slug}`, {
        json: def,
      })
      return res.json<Namespace>()
    },
    onSuccess: (updated) => {
      // Update cache after successful mutation
      client.setQueryData(namespaceKeys.detail(updated.slug), updated)
      // Invalidate list to refetch
      client.invalidateQueries({ queryKey: namespaceKeys.lists() })
    },
  })
}
```

## staleTime Configuration

```typescript
// Read-only data (never changes via mutations)
staleTime: Infinity

// Data that changes frequently
staleTime: 0  // Always stale

// Data refreshed occasionally
staleTime: 5 * 60 * 1000  // 5 minutes
```

## Using Queries in Components

```typescript
// Suspense (for prefetched data in loaders)
const { data: namespace } = useSuspenseQuery(getNamespaceQuery(slug))

// Regular query (client-side fetch)
const { data, isLoading, error } = useQuery(getNamespaceQuery(slug))
```

## Mutations with Optimistic Updates

```typescript
export const useRenameNamespace = () => {
  const client = useQueryClient()
  
  return useMutation({
    mutationFn: (params: { slug: string; newName: string }) =>
      request.patch(`api/namespaces/${params.slug}`, {
        json: { name: params.newName },
      }).json<Namespace>(),
    
    // Optimistic update
    onMutate: (vars) => {
      // Cancel in-flight queries
      client.cancelQueries({ queryKey: namespaceKeys.detail(vars.slug) })
      
      // Get previous data
      const previous = client.getQueryData(namespaceKeys.detail(vars.slug))
      
      // Update cache optimistically
      client.setQueryData(namespaceKeys.detail(vars.slug), (old: Namespace) => ({
        ...old,
        name: vars.newName,
      }))
      
      return { previous }
    },
    
    // Rollback on error
    onError: (err, vars, context) => {
      if (context?.previous) {
        client.setQueryData(
          namespaceKeys.detail(vars.slug),
          context.previous,
        )
      }
    },
    
    // Update on success
    onSuccess: (updated) => {
      client.setQueryData(namespaceKeys.detail(updated.slug), updated)
    },
  })
}
```

## Query Key Best Practices

```typescript
// Hierarchical structure
export const userKeys = {
  all: () => ['users'] as const,
  lists: () => [...userKeys.all(), 'list'] as const,
  list: (filters) => [...userKeys.lists(), filters] as const,
  details: () => [...userKeys.all(), 'detail'] as const,
  detail: (id) => [...userKeys.details(), id] as const,
}

// Invalidate related queries
client.invalidateQueries({ queryKey: userKeys.all() })  // All user queries
client.invalidateQueries({ queryKey: userKeys.lists() })  // Only list queries
client.invalidateQueries({ queryKey: userKeys.detail(id) })  // Specific user
```

## Infinite Queries

```typescript
export const useNamespacesInfinite = () =>
  useInfiniteQuery({
    queryKey: ['namespaces', 'infinite'],
    queryFn: ({ pageParam = 0 }) =>
      request.get(`api/namespaces?page=${pageParam}`).json(),
    getNextPageParam: (lastPage, pages) =>
      lastPage.hasMore ? pages.length : null,
  })

// In component
const { data, fetchNextPage, hasNextPage } = useNamespacesInfinite()

{data?.pages.map((page) =>
  page.items.map((ns) => <div key={ns.id}>{ns.name}</div>)
)}

<button onClick={() => fetchNextPage()} disabled={!hasNextPage}>
  Load More
</button>
```

## Verification Checklist

- [ ] All data fetching via hooks (no direct `ky` calls from components)
- [ ] Query keys hierarchical and consistent
- [ ] `staleTime` configured appropriately
- [ ] Mutations invalidate related queries
- [ ] Optimistic updates on mutations
- [ ] Error handling in mutations
- [ ] Loader prefetches with `ensureQueryData`
- [ ] Components use `useSuspenseQuery` (when prefetched)
- [ ] Cache invalidation strategy documented

