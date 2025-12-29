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
import { KeysetDecryptLabel } from '../messages';

import type { FormEvent } from 'react';
import type { Keyset, KeysetDecryptOperationResponse, Namespace } from '@konfigyr/hooks/types';

const decryptRequestSchema = z.object({
  ciphertext: z.string()
    .nonempty({ message: 'Text to decrypt can not be blank' })
    .base64url({ message: 'Text to decrypt must be a valid base64url encoded string'}),
  aad: z.string(),
});

function OperationResult({ plaintext, onClose }: KeysetDecryptOperationResponse & { onClose: () => void }) {
  const id = useId();

  return (
    <>
      <div className="grid gap-2">
        <Label htmlFor={`${id}-decryption-operation-result`}>
          <FormattedMessage
            defaultMessage="Decrypted text"
            description="Label for the textarea field that contains the decrypted text."
          />
        </Label>
        <Textarea
          id={`${id}-decryption-operation-result`}
          value={plaintext}
          readOnly
        />
      </div>
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onClose}>
          <CloseLabel />
        </Button>
        <ClipboardButton text={plaintext} />
      </div>
    </>
  );
}

export function KeysetDecryptOperation({ namespace, keyset, onCancel, onError }: {
  namespace: Namespace,
  keyset: Keyset,
  onCancel: () => void,
  onError: (error: Error) => void,
}) {
  const [response, setResponse] = useState<KeysetDecryptOperationResponse>();
  const { mutateAsync: decrypt } = useKeysetOperation(namespace.slug, keyset.id, 'decrypt');

  const form = useForm({
    defaultValues: { ciphertext: '', aad: '' },
    validators: {
      onSubmit: decryptRequestSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        const result = await decrypt(value);
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
      <OperationResult {...response} onClose={onCancel}/>
    );
  }

  return (
    <form.AppForm>
      <form name="keyset-encrypt-operation" className="grid gap-4" onSubmit={onSubmit}>
        <form.AppField name="ciphertext" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Text to decrypt"
              description="Label for the textarea field that contains the cipher text to be decrypted."
            />}
          >
            <field.Textarea rows={6}/>
          </field.Control>
        )} />

        <form.AppField name="aad" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage=" Additional authenticated data"
              description="Label for the textarea field that contains the additional authenticated data to be used when performing decryption operation."
            />}
            description={<FormattedMessage
              defaultMessage="Additional authenticated data is used as an integrity check and can help protect your data from a confused deputy attack."
              description="Helptext for the textarea field that explains how additional authenticated data is used when decrypting data."
            />}
          >
            <field.Textarea rows={4}/>
          </field.Control>
        )} />

        <div className="flex justify-end gap-2">
          <Button type="reset" variant="outline" onClick={onCancel}>
            <CancelLabel />
          </Button>
          <form.Submit>
            <KeysetDecryptLabel />
          </form.Submit>
        </div>
      </form>
    </form.AppForm>
  );
}
