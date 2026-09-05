import { queryOptions, useQuery } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { DashboardSummary } from './types';
import type { Namespace } from '../types';

/**
 * Keys used to store the namespace dashboard summary in the query client.
 */
export const dashboardKeys = {
  getDashboardSummary: (namespace: string) => ['namespace', namespace, 'dashboard'],
};

/**
 * Attempts to retrieve the dashboard summary for the given namespace from the Konfigyr API server.
 */
export const getDashboardQuery = (namespace: Namespace) => {
  return queryOptions({
    queryKey: dashboardKeys.getDashboardSummary(namespace.slug),
    queryFn: async ({ signal }): Promise<DashboardSummary> => {
      return await request.get(`api/namespaces/${namespace.slug}/dashboard`, { signal }).json<DashboardSummary>();
    },
    staleTime: 30_000,
  });
};

/**
 * Hook that retrieves the dashboard summary for the given namespace from the Konfigyr API server.
 */
export const useGetDashboard = (namespace: Namespace) => {
  return useQuery(getDashboardQuery(namespace));
};
