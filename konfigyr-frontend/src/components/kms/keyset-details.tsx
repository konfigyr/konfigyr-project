import { useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { parse as parseDuration, toDays } from 'duration-fns';
import { DeleteLabel } from '@konfigyr/components/messages';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardHeader,
} from '@konfigyr/components/ui/card';
import {
  KeysetAlgorithmLabel,
  KeysetDeletionGracePeriodHelpText,
  KeysetDeletionGracePeriodLabel,
  KeysetRotateLabel,
  KeysetRotationIntervalHelpText,
  KeysetRotationIntervalLabel,
} from './messages';
import { KeyTable } from './key-table';
import { KeysetAlgorithmDescription, KeysetAlgorithmName } from './keyset-algorithm';
import { KeyStatusAlert } from './key-status';
import { KeysetStateBadge } from './keyset-state';
import { KeysetOperationDialog } from './operation/keyset-operation-dialog';

import type { ReactNode } from 'react';
import type { Key, Keyset, Namespace } from '@konfigyr/hooks/types';
import type { OperationDialogOperation } from './operation/keyset-operation-dialog';

function KeysetCard({ label, value, description }: { label: ReactNode, value: ReactNode, description?: ReactNode }) {
  return (
    <div className="px-3">
      <p className="text-xs/relaxed font-medium text-muted-foreground uppercase">
        {label}
      </p>
      <p className="font-semibold text-base/loose">
        {value}
      </p>
      <p className="text-muted-foreground text-xs/relaxed">
        {description}
      </p>
    </div>
  );
}

function formatDurationToDays(duration: string) {
  const days = toDays(parseDuration(duration));
  return (
    <FormattedMessage
      defaultMessage="{days, plural, one {# day} other {# days}}"
      description="Label for number of days in a duration"
      values={{ days }}
    />
  );
}

function KeysetRotationInterval({ interval }: { interval?: string }) {
  if (interval) {
    return formatDurationToDays(interval);
  }

  return (
    <FormattedMessage
      defaultMessage="None, keys are never automatically replaced"
      description="Label for the disabled keyset rotation interval"
    />
  );
}

function KeysetDestructionGracePeriod({ interval }: { interval?: string }) {
  if (interval) {
    return formatDurationToDays(interval);
  }

  return (
    <FormattedMessage
      defaultMessage="None, keys are deleted immediately"
      description="Label for the disabled keyset destruction grace period"
    />
  );
}

export const KeysetDetails = ({ namespace, keyset, keys, onDelete, onChange }: {
  namespace: Namespace;
  keyset: Keyset;
  keys: Array<Key>;
  onDelete?: () => Promise<void>;
  onChange?: () => Promise<void>;
}) => {
  const [operation, setOperation] = useState<OperationDialogOperation | null>(null);
  const primary = keys.find(key => key.isPrimary);

  return (
    <div className="space-y-6">
      <div className="flex-col">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2">
            <p className="font-medium text-xl/relaxed">{keyset.name}</p>
            <KeysetStateBadge state={keyset.state} />
          </div>
          <div className="flex items-center gap-2">
            <Button variant="ghost" onClick={() => setOperation('rotate')}>
              <KeysetRotateLabel />
            </Button>
            <Button variant="destructive" onClick={() => setOperation('destroy')}>
              <DeleteLabel />
            </Button>
          </div>
        </div>
        {keyset.description && (
          <p className="text-muted-foreground text-sm/relaxed">
            {keyset.description}
          </p>
        )}
      </div>

      {primary && (
        <KeyStatusAlert status={primary.status} />
      )}

      <div className="grid grid-cols-3 gap-4 py-3 border rounded-xl *:border-r *:last:border-r-0">
        <KeysetCard
          label={<KeysetAlgorithmLabel />}
          value={<KeysetAlgorithmName algorithm={keyset.algorithm} />}
          description={<KeysetAlgorithmDescription algorithm={keyset.algorithm} />}
        />
        <KeysetCard
          label={<KeysetRotationIntervalLabel />}
          value={<KeysetRotationInterval interval={keyset.rotationInterval} />}
          description={<KeysetRotationIntervalHelpText />}
        />
        <KeysetCard
          label={<KeysetDeletionGracePeriodLabel />}
          value={<KeysetDestructionGracePeriod interval={keyset.destructionGracePeriod} />}
          description={<KeysetDeletionGracePeriodHelpText />}
        />
      </div>

      <Card className="border">
        <CardHeader
          title="Keys"
          description="A keyset holds one or more keys. The primary key is used for all new operations; older keys are kept to read data they previously protected."
        />
        <CardContent>
          <KeyTable
            namespace={namespace}
            keyset={keyset}
            keys={keys}
            onOperation={onChange}
          />
        </CardContent>
      </Card>

      {operation && (
        <KeysetOperationDialog
          namespace={namespace}
          keyset={keyset}
          operation={operation}
          onClose={() => {
            if (operation === 'destroy') {
              onDelete?.();
            } else {
              onChange?.();
            }

            setOperation(null);
          }}
        />
      )}
    </div>
  );
};
