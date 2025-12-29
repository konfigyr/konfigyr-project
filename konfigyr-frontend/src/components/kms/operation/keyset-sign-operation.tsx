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
import { KeysetSignLabel } from '../messages';

import type { FormEvent } from 'react';
import type { Keyset, Namespace, SignatureAware } from '@konfigyr/hooks/types';

const signRequestSchema = z.object({
  plaintext: z.string()
    .nonempty({ message: 'Text to sign can not be blank' }),
});

function OperationResult({ signature, onClose }: SignatureAware & { onClose: () => void }) {
  const id = useId();

  return (
    <>
      <div className="grid gap-2">
        <Label htmlFor={`${id}-signing-operation-result`}>
          <FormattedMessage
            defaultMessage="Signature"
            description="Label for the field that contains the digital signature."
          />
        </Label>
        <Textarea
          id={`${id}-signing-operation-result`}
          value={signature}
          rows={6}
          readOnly
        />
      </div>
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onClose}>
          <CloseLabel />
        </Button>
        <ClipboardButton text={signature} />
      </div>
    </>
  );
}

export function KeysetSigningOperation({ namespace, keyset, onCancel, onError }: {
  namespace: Namespace,
  keyset: Keyset,
  onCancel: () => void,
  onError: (error: Error) => void,
}) {
  const [response, setResponse] = useState<SignatureAware>();
  const { mutateAsync: encrypt } = useKeysetOperation(namespace.slug, keyset.id, 'sign');

  const form = useForm({
    defaultValues: { plaintext: '' },
    validators: {
      onSubmit: signRequestSchema,
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
              defaultMessage="Text to sign"
              description="Label for the textarea field that contains the plain text to be digitally signed."
            />}
          >
            <field.Textarea rows={6}/>
          </field.Control>
        )} />

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="reset" variant="outline" onClick={onCancel}>
            <CancelLabel />
          </Button>
          <form.Submit>
            <KeysetSignLabel />
          </form.Submit>
        </div>
      </form>
    </form.AppForm>
  );
}
