import { queryOptions, useQuery } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { AuditRecord, AuditRecordQuery } from './types';
import type { CursorResponse } from '../hateoas/types';
import type { Namespace } from '../namespace/types';

/**
 * Keys used to store the audit log records in the query client.
 */
export const auditKeys = {
  getAuditRecords: (namespace: string, query: AuditRecordQuery) => ['namespace', namespace, 'audit-records', query],
};

/**
 * Attempts to retrieve the audit log records for the given namespace from the Konfigyr API server.
 */
export const getAuditRecordsQuery = (namespace: Namespace, query: AuditRecordQuery) => {
  return queryOptions({
    queryKey: auditKeys.getAuditRecords(namespace.slug, query),
    queryFn: ({ signal }) => {
      return request.get(`api/namespaces/${namespace.slug}/audit`, { signal, searchParams: { ...query } })
        .json<CursorResponse<AuditRecord>>();
    },
    placeholderData: previousData => previousData,
    staleTime: 5000,
  });
};

/**
 * Hook that retrieves the audit log records for the given namespace from the Konfigyr API server.
 */
export const useGetAuditRecords = (namespace: Namespace, query: AuditRecordQuery) => {
  return useQuery(getAuditRecordsQuery(namespace, query));
};
