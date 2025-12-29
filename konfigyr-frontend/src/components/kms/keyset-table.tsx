import { useCallback, useState } from 'react';
import {
  EllipsisVerticalIcon,
  FolderLockIcon,
} from 'lucide-react';
import { FormattedDate, FormattedMessage } from 'react-intl';
import { ErrorState } from '@konfigyr/components/error';
import { CreatedAtLabel, DeleteLabel, UpdatedAtLabel } from '@konfigyr/components/messages';
import { Badge } from '@konfigyr/components/ui/badge';
import { Button } from '@konfigyr/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@konfigyr/components/ui/table';
import { KeysetAlgorithmName } from './keyset-algorithm';
import { KeysetState } from './keyset-state';
import {
  KeysetAlgorithmLabel,
  KeysetDecryptLabel,
  KeysetDisableLabel,
  KeysetEncryptLabel,
  KeysetNameLabel,
  KeysetReactivateLabel,
  KeysetRotateLabel,
  KeysetSignLabel,
  KeysetStateLabel,
  KeysetVerifySignatureLabel,
} from './messages';
import { KeysetOperationDialog } from './operation/keyset-operation-dialog';

import type { Keyset, Namespace } from '@konfigyr/hooks/types';
import type { OperationDialogOperation } from './operation/keyset-operation-dialog';

interface SelectedOperation {
  keyset: Keyset,
  operation: OperationDialogOperation,
}

function useOperationSelectedCallback(
  keyset: Keyset,
  operation: OperationDialogOperation,
  onOperationSelected: (operation: SelectedOperation) => void,
) {
  return useCallback(() => {
    onOperationSelected({ operation, keyset });
  }, [operation, onOperationSelected, keyset]);
}

function KeysetDropdownMenu({ keyset, onOperationSelected }: { keyset: Keyset, onOperationSelected: (operation: SelectedOperation) => void }) {
  const onEncrypt = useOperationSelectedCallback(keyset, 'encrypt', onOperationSelected);
  const onDecrypt = useOperationSelectedCallback(keyset, 'decrypt', onOperationSelected);
  const onSign = useOperationSelectedCallback(keyset, 'sign', onOperationSelected);
  const onVerify = useOperationSelectedCallback(keyset, 'verify', onOperationSelected);
  const onReactivate = useOperationSelectedCallback(keyset, 'reactivate', onOperationSelected);
  const onRotate = useOperationSelectedCallback(keyset, 'rotate', onOperationSelected);
  const onDisable = useOperationSelectedCallback(keyset, 'disable', onOperationSelected);
  const onDestroy = useOperationSelectedCallback(keyset, 'destroy', onOperationSelected);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button size="sm" variant="ghost">
          <EllipsisVerticalIcon size="1rem" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-68">
        <DropdownMenuGroup>
          <DropdownMenuItem>Edit</DropdownMenuItem>
          <DropdownMenuItem disabled={keyset.state !== 'ACTIVE'} onClick={onRotate}>
            <KeysetRotateLabel />
          </DropdownMenuItem>

          {keyset.state === 'INACTIVE' && (
            <DropdownMenuItem onClick={onReactivate}>
              <KeysetReactivateLabel />
            </DropdownMenuItem>
          )}

          {keyset.state === 'ACTIVE' && (
            <DropdownMenuItem onClick={onDisable}>
              <KeysetDisableLabel />
            </DropdownMenuItem>
          )}
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <DropdownMenuItem disabled={keyset.state !== 'ACTIVE'} onClick={onEncrypt}>
            <KeysetEncryptLabel />
          </DropdownMenuItem>
          <DropdownMenuItem disabled={keyset.state !== 'ACTIVE'} onClick={onDecrypt}>
            <KeysetDecryptLabel />
          </DropdownMenuItem>
          <DropdownMenuItem disabled={keyset.state !== 'ACTIVE'} onClick={onSign}>
            <KeysetSignLabel />
          </DropdownMenuItem>
          <DropdownMenuItem disabled={keyset.state !== 'ACTIVE'} onClick={onVerify}>
            <KeysetVerifySignatureLabel />
          </DropdownMenuItem>
          <DropdownMenuSeparator />
        </DropdownMenuGroup>
        <DropdownMenuGroup>
          <DropdownMenuItem variant="destructive" onClick={onDestroy}>
            <DeleteLabel />
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function KeysetRow({ keyset, onOperationSelected }: { keyset: Keyset, onOperationSelected: (operation: SelectedOperation) => void }) {
  return (
    <TableRow key={keyset.id}>
      <TableCell>
        <p className="text-sm leading-snug font-medium">
          {keyset.name}
        </p>
        <p className="text-muted-foreground line-clamp-2 text-sm leading-normal font-normal text-balance">
          {keyset.description}
        </p>
      </TableCell>
      <TableCell>
        <Badge variant="outline" size="sm">
          <KeysetAlgorithmName algorithm={keyset.algorithm} />
        </Badge>
      </TableCell>
      <TableCell>
        <Badge variant="outline" size="sm">
          <KeysetState state={keyset.state} />
        </Badge>
      </TableCell>
      <TableCell>
        <FormattedDate value={keyset.createdAt} />
      </TableCell>
      <TableCell>
        <FormattedDate value={keyset.updatedAt} />
      </TableCell>
      <TableCell>
        <KeysetDropdownMenu
          keyset={keyset}
          onOperationSelected={onOperationSelected}
        />
      </TableCell>
    </TableRow>
  );
}

function KeysetSkeleton() {
  return (
    <TableRow data-slot="keyset-loading-skeleton">
      <TableCell className="flex flex-col gap-2">
        <Skeleton className="w-24 h-4" />
        <Skeleton className="w-46 h-4" />
      </TableCell>
      <TableCell>
        <Skeleton className="w-24 h-4" />
      </TableCell>
      <TableCell>
        <Skeleton className="w-24 h-4" />
      </TableCell>
      <TableCell>
        <Skeleton className="w-24 h-4" />
      </TableCell>
      <TableCell colSpan={2}>
        <Skeleton className="w-24 h-4" />
      </TableCell>
    </TableRow>
  );
}

function LoadingSkeleton() {
  return (
    <>
      <KeysetSkeleton />
      <KeysetSkeleton />
      <KeysetSkeleton />
    </>
  );
}

export function KeysetTable({ namespace, keysets, error, isPending }: {
  namespace: Namespace,
  keysets?: Array<Keyset>,
  error?: Error | null,
  isPending?: boolean,
}) {
  const [operation, setOperation] = useState<null | SelectedOperation>(null);

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="min-w-64">
              <KeysetNameLabel />
            </TableHead>
            <TableHead className="w-48">
              <KeysetAlgorithmLabel />
            </TableHead>
            <TableHead className="w-32">
              <KeysetStateLabel />
            </TableHead>
            <TableHead className="w-32">
              <CreatedAtLabel />
            </TableHead>
            <TableHead className="w-32">
              <UpdatedAtLabel />
            </TableHead>
            <TableHead className="w-6"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isPending && (
            <LoadingSkeleton />
          )}

          {error && (
            <tr>
              <td colSpan={6} className="p-6">
                <ErrorState error={error} />
              </td>
            </tr>
          )}

          {(keysets && keysets.length === 0) && (
            <tr>
              <td colSpan={6}>
                <EmptyState
                  icon={<FolderLockIcon size="2rem" />}
                  title={<FormattedMessage
                    defaultMessage="No keysets found"
                    description="Empty state title when no KMS keysets are found."
                  />}
                  description={<FormattedMessage
                    defaultMessage="We could not find any keysets matching your search criteria. Why don't you create one?"
                    description="Empty state description when no KMS keysets are found."
                  />}
                />
              </td>
            </tr>
          )}

          {keysets?.map((keyset) => (
            <KeysetRow
              key={keyset.id}
              keyset={keyset}
              onOperationSelected={setOperation}
            />
          ))}
        </TableBody>
      </Table>

      {operation && (
        <KeysetOperationDialog
          namespace={namespace}
          keyset={operation.keyset}
          operation={operation.operation}
          onClose={() => setOperation(null)}
        />
      )}
    </>
  );

}
