import { useCallback, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { useErrorNotification } from '@konfigyr/components/error';
import {
  KeysetDecryptLabel,
  KeysetEncryptLabel,
  KeysetReactivateLabel,
  KeysetSignLabel,
  KeysetVerifySignatureLabel,
} from '@konfigyr/components/kms/messages';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@konfigyr/components/ui/dialog';
import { KeysetDecryptOperation } from './keyset-decrypt-operation';
import { KeysetDestroyOperation } from './keyset-destroy-operation';
import { KeysetDisableOperation } from './keyset-disable-operation';
import { KeysetEncryptOperation } from './keyset-encrypt-operation';
import { KeysetReactivateOperation } from './keyset-reactivate-operation';
import { KeysetRotateOperation } from './keyset-rotate-operation';
import { KeysetSigningOperation } from './keyset-sign-operation';
import { KeysetVerifySignatureOperation } from './keyset-verify-operation';

import type { Namespace } from '@konfigyr/hooks/namespace/types';
import type { Keyset, KeysetOperation } from '@konfigyr/hooks/kms/types';

export type OperationDialogOperation = KeysetOperation | 'rotate' | 'reactivate' | 'disable' | 'destroy';

function OperationDialogTitle({ operation }: { operation: OperationDialogOperation }) {
  switch (operation) {
    case 'encrypt':
      return <KeysetEncryptLabel />;
    case 'decrypt':
      return <KeysetDecryptLabel />;
    case 'sign':
      return <KeysetSignLabel />;
    case 'verify':
      return <KeysetVerifySignatureLabel />;
    case 'reactivate':
      return <KeysetReactivateLabel />;
    case 'rotate':
      return <FormattedMessage
        defaultMessage="Generate a new primary key now?"
        description="Title of the KMS key rotation operation confirmation dialog."
      />;
    case 'disable':
      return <FormattedMessage
        defaultMessage="Are you sure you want to disable this keyset?"
        description="Title of the KMS keyset disable operation confirmation dialog."
      />;
    case 'destroy':
      return <FormattedMessage
        defaultMessage="Are you sure you want to destroy this keyset?"
        description="Title of the KMS keyset destroy operation confirmation dialog."
      />;
    default:
      throw new Error(`Unsupported operation: ${operation}`);
  }
}

function OperationDialogDescription({ operation }: { operation: OperationDialogOperation }) {
  switch (operation) {
    case 'encrypt':
      return (
        <FormattedMessage
          defaultMessage="The operation will return a ciphertext that is Base64URL encoded and can be decrypted using the same keyset."
          description="Description of the encrypt operation dialog."
        />
      );
    case 'decrypt':
      return (
        <FormattedMessage
          defaultMessage="The operation will return the original plaintext that was encrypted using the same keyset."
          description="Description of the decrypt operation dialog."
        />
      );
    case 'sign':
      return (
        <FormattedMessage
          defaultMessage="This operation will generate a Base64URL encoded signature."
          description="Description of the signign operation dialog."
        />
      );
    case 'verify':
      return (
        <FormattedMessage
          defaultMessage="Verify the signature of data using the selected keyset. The operation will return true if the signature is valid, false otherwise."
          description="Description of the signature verification operation dialog."
        />
      );
    case 'reactivate':
      return (
        <FormattedMessage
          defaultMessage="This operation restores the keyset to an active state, permitting all associated cryptographic functions."
          description="Description of the keyset reactivation operation dialog."
        />
      );
    case 'rotate':
      return (
        <FormattedMessage
          defaultMessage="Generates a new primary key for future operations. The previous key is retained for decryption and signature verification."
          description="Description of the key rotation operation dialog."
        />
      );
    case 'disable':
      return (
        <FormattedMessage
          defaultMessage="This operation would prevent any cryptographic operations using the keyset. The cryptographic material would not be removed from the system and can be re-enabled at any time."
          description="Description of the keyset disable operation dialog."
        />
      );
    case 'destroy':
      return (
        <FormattedMessage
          defaultMessage="Destroying this keyset will permanently wipe all cryptographic material from the system. Any data encrypted with these keys will become irrecoverable."
          description="Description of the keyset destroy operation dialog."
        />
      );
    default:
      throw new Error(`Unsupported operation: ${operation}`);
  }
}

export function KeysetOperationDialog({ namespace, keyset, operation, onClose }: {
  namespace: Namespace,
  keyset: Keyset,
  operation: OperationDialogOperation,
  onClose?: () => void,
}) {
  const [open, onOpenChange] = useState(true);
  const onError = useErrorNotification();

  const onCloseDialog = useCallback(() => {
    onOpenChange(false);
    const timeout = setTimeout(() => onClose?.(), 200);
    return () => clearTimeout(timeout);
  }, [onOpenChange, onClose]);

  return (
    <Dialog open={open} onOpenChange={onCloseDialog}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            <OperationDialogTitle operation={operation} />
          </DialogTitle>
          <DialogDescription>
            <OperationDialogDescription operation={operation} />
          </DialogDescription>
        </DialogHeader>

        {operation === 'encrypt' && (
          <KeysetEncryptOperation
            namespace={namespace}
            keyset={keyset}
            onCancel={onCloseDialog}
            onError={onError}
          />
        )}

        {operation === 'decrypt' && (
          <KeysetDecryptOperation
            namespace={namespace}
            keyset={keyset}
            onCancel={onCloseDialog}
            onError={onError}
          />
        )}

        {operation === 'sign' && (
          <KeysetSigningOperation
            namespace={namespace}
            keyset={keyset}
            onCancel={onCloseDialog}
            onError={onError}
          />
        )}

        {operation === 'verify' && (
          <KeysetVerifySignatureOperation
            namespace={namespace}
            keyset={keyset}
            onCancel={onCloseDialog}
            onError={onError}
          />
        )}

        {operation === 'reactivate' && (
          <KeysetReactivateOperation
            namespace={namespace}
            keyset={keyset}
            onCancel={onCloseDialog}
          />
        )}

        {operation === 'rotate' && (
          <KeysetRotateOperation
            namespace={namespace}
            keyset={keyset}
            onCancel={onCloseDialog}
          />
        )}

        {operation === 'disable' && (
          <KeysetDisableOperation
            namespace={namespace}
            keyset={keyset}
            onCancel={onCloseDialog}
          />
        )}

        {operation === 'destroy' && (
          <KeysetDestroyOperation
            namespace={namespace}
            keyset={keyset}
            onCancel={onCloseDialog}
          />
        )}

      </DialogContent>
    </Dialog>
  );
}
