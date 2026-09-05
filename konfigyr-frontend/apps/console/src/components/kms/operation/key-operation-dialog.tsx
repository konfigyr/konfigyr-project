import { useCallback, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@konfigyr/components/ui/dialog';
import { KeyCompromisedOperation } from './key-compromised-operation';
import { KeyDestroyOperation } from './key-destroy-operation';
import { KeyDisableOperation } from './key-disable-operation';
import { KeyReactivateOperation } from './key-reactivate-operation';
import { KeyRestoreOperation } from './key-restore-operation';

import type { FunctionComponent } from 'react';
import type { Namespace } from '@konfigyr/hooks/namespace/types';
import type { Key, Keyset } from '@konfigyr/hooks/kms/types';

export type OperationDialogOperation = 'deactivate' | 'reactivate' | 'compromise' | 'restore' | 'destroy';

function OperationDialogTitle({ operation }: { operation: OperationDialogOperation }) {
  switch (operation) {
    case 'deactivate':
      return <FormattedMessage
        defaultMessage="Deactivate this key?"
        description="Title of the KMS key deactivate operation confirmation dialog."
      />;
    case 'reactivate':
      return <FormattedMessage
        defaultMessage="Restore key?"
        description="Title of the KMS key reactivate operation confirmation dialog."
      />;
    case 'compromise':
      return <FormattedMessage
        defaultMessage="Mark key as compromised?"
        description="Title of the KMS key compromised operation confirmation dialog."
      />;
    case 'restore':
      return <FormattedMessage
        defaultMessage="Restore key?"
        description="Title of the KMS restore key operation confirmation dialog."
      />;
    case 'destroy':
      return <FormattedMessage
        defaultMessage="Are you sure you want to destroy this key?"
        description="Title of the KMS key destroy operation confirmation dialog."
      />;
    default:
      throw new Error(`Unsupported operation: ${operation}`);
  }
}

function OperationDialogDescription({ operation }: { operation: OperationDialogOperation }) {
  switch (operation) {
    case 'deactivate':
      return (
        <FormattedMessage
          defaultMessage="This key will no longer be used for new operations. Existing data it protected will remain readable, but you will need to reactivate it before it can encrypt or sign anything new."
          description="Description of the deactivate key operation dialog."
        />
      );
    case 'reactivate':
      return (
        <FormattedMessage
          defaultMessage="This key will resume being used for new cryptographic operations. Make sure it has not been compromised before reactivating. If you have any doubt, rotate the keyset instead."
          description="Description of the reactivate key operation dialog."
        />
      );
    case 'compromise':
      return (
        <FormattedMessage
          defaultMessage="This immediately disables the key and flags it as untrusted. Any data it was used to protect should be considered at risk and re-encrypted with a new key. This action cannot be undone."
          description="Description of the compromise key operation dialog."
        />
      );
    case 'restore':
      return (
        <FormattedMessage
          defaultMessage="This will cancel the scheduled destruction and move the key back to a disabled state. No data will be lost. The key will not resume operations until it is explicitly reactivated."
          description="Description of the restore key operation dialog."
        />
      );
    case 'destroy':
      return (
        <FormattedMessage
          defaultMessage="The key will enter a pending deletion period, after which its material will be permanently erased and all data it protected will become unreadable. Once destroyed, there is no recovery. Make sure all data this key protects has been re-encrypted before proceeding."
          description="Description of the destroy key operation dialog."
        />
      );
    default:
      throw new Error(`Unsupported operation: ${operation}`);
  }
}

function componentForOperation(operation: OperationDialogOperation): FunctionComponent<{
  namespace: Namespace;
  value: Key;
  keyset: Keyset;
  onCancel: () => void;
}> {
  switch (operation) {
    case 'deactivate':
      return KeyDisableOperation;
    case 'reactivate':
      return KeyReactivateOperation;
    case 'compromise':
      return KeyCompromisedOperation;
    case 'restore':
      return KeyRestoreOperation;
    case 'destroy':
      return KeyDestroyOperation;
    default:
      throw new Error(`Unsupported operation: ${operation}`);
  }
}

export function KeyOperationDialog({ namespace, keyset, value: key, operation, onClose }: {
  namespace: Namespace,
  keyset: Keyset,
  value: Key,
  operation: OperationDialogOperation,
  onClose?: () => void,
}) {
  const [open, onOpenChange] = useState(true);
  const Contents = componentForOperation(operation);

  const onCloseDialog = useCallback(() => {
    onOpenChange(false);
    const timeout = setTimeout(() => onClose?.(), 200);
    return () => clearTimeout(timeout);
  }, [onOpenChange, onClose]);

  return (
    <Dialog open={open} onOpenChange={onCloseDialog}>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>
            <OperationDialogTitle operation={operation} />
          </DialogTitle>
          <DialogDescription className="leading-normal">
            <OperationDialogDescription operation={operation} />
          </DialogDescription>
        </DialogHeader>

        <Contents
          namespace={namespace}
          keyset={keyset}
          value={key}
          onCancel={onCloseDialog}
        />
      </DialogContent>
    </Dialog>
  );
}
