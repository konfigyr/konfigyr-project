import { z } from 'zod';
import { useCallback, useId, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { useKeysetOperation } from '@konfigyr/hooks';
import { ClipboardButton } from '@konfigyr/components/clipboard';
import { CancelLabel, CloseLabel } from '@konfigyr/components/messages';
import { Button } from '@konfigyr/components/ui/button';
import { Label } from '@konfigyr/components/ui/label';
import { useForm } from '@konfigyr/components/ui/form';
import { Textarea } from '@konfigyr/components/ui/textarea';
import { KeysetEncryptLabel } from '../messages';

import type { FormEvent } from 'react';
import type { Keyset, KeysetEncryptOperationResponse, Namespace } from '@konfigyr/hooks/types';

const encryptRequestSchema = z.object({
  plaintext: z.string()
    .nonempty({ message: 'Text to encrypt can not be blank' }),
  aad: z.string(),
});

function OperationResult({ ciphertext, checksum, onClose }: KeysetEncryptOperationResponse & { onClose: () => void }) {
  const id = useId();

  return (
    <>
      <div className="grid gap-2">
        <Label htmlFor={`${id}-encryption-operation-ciphertext`}>
          <FormattedMessage
            defaultMessage="Encrypted text"
            description="Label for the textarea field that contains the encrypted text as a result of the encryption operation."
          />
        </Label>
        <Textarea
          id={`${id}-encryption-operation-ciphertext`}
          value={ciphertext}
          readOnly
        />
      </div>
      <div className="grid gap-2">
        <Label htmlFor={`${id}-encryption-operation-checksum`}>
          <FormattedMessage
            defaultMessage="Checksum"
            description="Label for the field that contains the checksum of the encrypted text."
          />
        </Label>
        <Textarea
          id={`${id}-encryption-operation-checksum`}
          value={checksum}
          readOnly
        />
      </div>
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onClose}>
          <CloseLabel />
        </Button>
        <ClipboardButton text={ciphertext} />
      </div>
    </>
  );
}

export function KeysetEncryptOperation({ namespace, keyset, onCancel, onError }: {
  namespace: Namespace,
  keyset: Keyset,
  onCancel: () => void
  onError: (error: Error) => void,
}) {
  const [response, setResponse] = useState<KeysetEncryptOperationResponse>();
  const { mutateAsync: encrypt } = useKeysetOperation(namespace.slug, keyset.id, 'encrypt');

  const form = useForm({
    defaultValues: { plaintext: '', aad: '' },
    validators: {
      onSubmit: encryptRequestSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        const result = await encrypt(value);
        form.reset();

        setResponse(result);
      } catch (error) {
        onError(error as Error);
      }
    },
  });

  const onSubmit = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    event.stopPropagation();

    return form.handleSubmit(event);
  }, [form.handleSubmit]);

  if (response) {
    return (
      <OperationResult {...response} onClose={onCancel} />
    );
  }

  return (
    <form.AppForm>
      <form name="keyset-encrypt-operation" className="grid gap-4" onSubmit={onSubmit}>
        <form.AppField name="plaintext" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Text to encrypt"
              description="Label for the textarea field that contains the plain text to be encrypted."
            />}
          >
            <field.Textarea rows={6}/>
          </field.Control>
        )} />

        <form.AppField name="aad" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage=" Additional authenticated data"
              description="Label for the textarea field that contains the additional authenticated data to be used when performing encryption operation."
            />}
            description={<FormattedMessage
              defaultMessage="Additional authenticated data is used as an integrity check and can help protect your data from a confused deputy attack."
              description="Helptext for the textarea field that explains how additional authenticated data is used when encrypting data."
            />}
          >
            <field.Textarea rows={4}/>
          </field.Control>
        )} />

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="reset" variant="outline" onClick={onCancel}>
            <CancelLabel />
          </Button>
          <form.Submit>
            <KeysetEncryptLabel />
          </form.Submit>
        </div>
      </form>
    </form.AppForm>
  );
}
