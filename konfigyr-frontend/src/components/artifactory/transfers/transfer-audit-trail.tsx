import { HistoryLabel } from '@konfigyr/components/messages';
import { AuditRecordListContent } from '@konfigyr/components/audit/audit-record-list';
import { useGetAuditRecords } from '@konfigyr/hooks';

import type { Namespace } from '@konfigyr/hooks/types';

export function TransferAuditTrail({ namespace, transferId }: {
  namespace: Namespace;
  transferId: string;
}) {
  const { data, error, isPending } = useGetAuditRecords(namespace, {
    entityType: 'artifact-ownership-transfer',
    entityId: transferId,
    size: 20,
  });

  return (
    <div className="space-y-3">
      <h2 className="text-sm font-medium text-muted-foreground">
        <HistoryLabel/>
      </h2>
      <AuditRecordListContent data={data} error={error} isPending={isPending}/>
    </div>
  );
}
