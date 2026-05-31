import { useCallback, useState } from 'react';
import {
  BanIcon,
  CalendarX2Icon,
  CircleCheckIcon,
  EllipsisVerticalIcon,
  StarIcon,
  TriangleAlertIcon,
  UndoDotIcon,
} from 'lucide-react';
import { FormattedDate, FormattedMessage } from 'react-intl';
import { RelativeDate } from '@konfigyr/components/messages/relative-date';
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@konfigyr/components/ui/table';
import { KeyOperationDialog } from './operation/key-operation-dialog';
import {
  KeyCompromisedLabel,
  KeyDeactivateLabel,
  KeyDestroyLabel,
  KeyReactivateLabel,
  KeyRestoreLabel,
  KeysetAlgorithmLabel,
  KeysetStateLabel,
} from './messages';
import { KeyStatusBadge } from './key-status';
import { KeysetAlgorithmName } from './keyset-algorithm';

import type { Key, Keyset, Namespace } from '@konfigyr/hooks/types';
import type { OperationDialogOperation } from './operation/key-operation-dialog';

const useOperationSelectedCallback = (
  operation: OperationDialogOperation,
  onOperationSelected: (operation: OperationDialogOperation) => void,
) => useCallback(() => onOperationSelected(operation), [operation, onOperationSelected]);

function KeysetDropdownMenu({ value: key, onSelect }: { value: Key, onSelect: (operation: OperationDialogOperation) => void }) {
  const onDisable = useOperationSelectedCallback('deactivate', onSelect);
  const onReactivate = useOperationSelectedCallback('reactivate', onSelect);
  const onCompromise = useOperationSelectedCallback('compromise', onSelect);
  const onRestore = useOperationSelectedCallback('restore', onSelect);
  const onDestroy = useOperationSelectedCallback('destroy', onSelect);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button size="sm" variant="ghost">
            <EllipsisVerticalIcon size="1rem" />
            <span className="sr-only">
              <FormattedMessage
                defaultMessage="Open actions menu for key {key}"
                description="Label used by screen readers to open the dropdown dialog to manage KMS key inside a keyset."
                values={{ key: key.id }}
              />
            </span>
          </Button>
        }
      />
      <DropdownMenuContent className="w-68">
        <DropdownMenuGroup>
          <DropdownMenuItem onClick={onDisable} disabled={key.status !== 'ENABLED'}>
            <BanIcon /> <KeyDeactivateLabel />
          </DropdownMenuItem>

          <DropdownMenuItem onClick={onReactivate} disabled={key.status !== 'DISABLED'}>
            <CircleCheckIcon /> <KeyReactivateLabel />
          </DropdownMenuItem>

          <DropdownMenuItem onClick={onRestore} disabled={key.status !== 'PENDING_DESTRUCTION'}>
            <UndoDotIcon /> <KeyRestoreLabel />
          </DropdownMenuItem>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <DropdownMenuItem
            variant="destructive"
            disabled={key.status === 'COMPROMISED' || key.status === 'PENDING_DESTRUCTION' || key.status === 'DESTROYED'}
            onClick={onCompromise}
          >
            <TriangleAlertIcon /> <KeyCompromisedLabel />
          </DropdownMenuItem>
          <DropdownMenuItem
            variant="destructive"
            disabled={key.status === 'PENDING_DESTRUCTION' || key.status === 'DESTROYED'}
            onClick={onDestroy}
          >
            <CalendarX2Icon /> <KeyDestroyLabel />
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function ExpiryCell({ value: key }: { value: Key }) {
  if (!key.expiresAt) return (
    <p className="text-xs text-muted-foreground">
      <FormattedMessage
        defaultMessage="Never"
        description="Key expiry label when it never expires."
      />
    </p>
  );

  const date = new Date(key.expiresAt);

  if (date.getTime() > Date.now()) {
    return (
      <>
        <time className="font-medium" dateTime={key.expiresAt}>
          <FormattedDate value={date} />
        </time>
        <p className="text-xs text-muted-foreground">
          <FormattedMessage
            defaultMessage="Expires"
            description="Key expiry label when it expires in the future."
          />
        </p>
      </>
    );
  }

  return (
    <>
      <time className="font-medium text-muted-foreground" dateTime={key.expiresAt}>
        <FormattedDate value={date} />
      </time>
      <p className="text-xs text-muted-foreground">
        <FormattedMessage
          defaultMessage="Expired"
          description="Key expiry label when it is expired."
        />
      </p>
    </>
  );
}

function DestructionCell({ value: key }: { value: Key }) {
  if (key.status === 'DESTROYED') {
    return (
      <>
        <time className="font-medium text-destructive" dateTime={key.destroyedAt}>
          <FormattedDate value={key.destroyedAt} />
        </time>
        <p className="text-xs text-muted-foreground">
          <FormattedMessage
            defaultMessage="Destroyed"
            description="Key destruction state label when it has been destroyed."
          />
        </p>
      </>
    );
  }

  if (key.status === 'PENDING_DESTRUCTION') {
    return (
      <>
        <time className="font-medium text-amber-500" dateTime={key.destructionScheduledAt}>
          <FormattedDate value={key.destructionScheduledAt} />
        </time>
        <p className="text-xs text-muted-foreground">
          <FormattedMessage
            defaultMessage="Scheduled"
            description="Key destruction state label when it has been scheduled for destruction."
          />
        </p>
      </>
    );
  }

  return '-';
}

interface SelectedOperation {
  key: Key,
  operation: OperationDialogOperation,
}

export function KeyTable({ keyset, keys, namespace, onOperation }: {
  keyset: Keyset;
  keys: Array<Key>;
  namespace: Namespace;
  onOperation?: (key: Key, operation: OperationDialogOperation) => void;
}) {
  const [selected, setSelected] = useState<SelectedOperation | null>(null);

  const onOperationDialogClose = useCallback(() => {
    if (selected) {
      onOperation?.(selected.key, selected.operation);
      setSelected(null);
    }
  }, [selected, setSelected, onOperation]);

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>
              <FormattedMessage
                defaultMessage="Key ID"
                description="KMS Keyset key ID label."
              />
            </TableHead>
            <TableHead>
              <KeysetAlgorithmLabel />
            </TableHead>
            <TableHead>
              <KeysetStateLabel />
            </TableHead>
            <TableHead>
              <FormattedMessage
                defaultMessage="Created"
                description="KMS Keyset key creation date label."
              />
            </TableHead>
            <TableHead>
              <FormattedMessage
                defaultMessage="Expiry"
                description="KMS Keyset key expiry date label."
              />
            </TableHead>
            <TableHead colSpan={2}>
              <FormattedMessage
                defaultMessage="Destruction"
                description="KMS Keyset key destruction state label."
              />
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {keys.map(key => (
            <TableRow key={key.id}>
              <TableCell>
                <p className="flex items-center gap-2">
                  <span>{key.id}</span>
                  {key.isPrimary && (
                    <Badge variant="info" size="sm">
                      <StarIcon /> Primary
                    </Badge>
                  )}
                </p>
              </TableCell>
              <TableCell>
                <Badge variant="outline">
                  <KeysetAlgorithmName algorithm={key.algorithm} />
                </Badge>
              </TableCell>
              <TableCell>
                <KeyStatusBadge status={key.status} />
              </TableCell>
              <TableCell>
                <RelativeDate value={key.createdAt} />
              </TableCell>
              <TableCell>
                <ExpiryCell value={key} />
              </TableCell>
              <TableCell>
                <DestructionCell value={key} />
              </TableCell>
              <TableCell>
                <KeysetDropdownMenu
                  value={key}
                  onSelect={operation => setSelected({ key, operation })}
                />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>

        {selected && (
          <KeyOperationDialog
            namespace={namespace}
            keyset={keyset}
            value={selected.key}
            operation={selected.operation}
            onClose={onOperationDialogClose}
          />
        )}
      </Table>
    </>
  );
}
