import { z } from 'zod';
import { useCallback, useState } from 'react';
import { AlertCircleIcon, CheckCircle2Icon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useKeysetOperation } from '@konfigyr/hooks';
import { CancelLabel } from '@konfigyr/components/messages';
import { Button } from '@konfigyr/components/ui/button';
import { Alert, AlertTitle } from '@konfigyr/components/ui/alert';
import { useForm } from '@konfigyr/components/ui/form';
import { KeysetVerifySignatureLabel } from '../messages';

import type { FormEvent } from 'react';
import type { Keyset, KeysetVerificationOperationResponse, Namespace } from '@konfigyr/hooks/types';

const verifySignatureRequestSchema = z.object({
  plaintext: z.string()
    .nonempty({ message: 'Text to sign can not be blank' }),
  signature: z.string()
    .nonempty({ message: 'Signature can not be blank' })
    .base64url({ message: 'Signature must be a valid base64url encoded string' }),
});

function OperationResult({ valid }: { valid?: boolean }) {
  if (valid === undefined) {
    return null;
  }

  return (
    <Alert variant={valid ? 'default' : 'destructive'}>
      {valid ? <CheckCircle2Icon/> : <AlertCircleIcon />}
      <AlertTitle>
        {valid ? 'Signature is valid' : 'Signature is invalid'}
      </AlertTitle>
    </Alert>
  );
}

export function KeysetVerifySignatureOperation({ namespace, keyset, onCancel, onError }: {
  namespace: Namespace,
  keyset: Keyset,
  onCancel: () => void,
  onError: (error: Error) => void,
}) {
  const [response, setResponse] = useState<KeysetVerificationOperationResponse>();
  const { mutateAsync: encrypt } = useKeysetOperation(namespace.slug, keyset.id, 'verify');

  const form = useForm({
    defaultValues: { plaintext: '', signature: '' },
    validators: {
      onSubmit: verifySignatureRequestSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        const result = await encrypt(value);

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

  return (
    <form.AppForm>
      <form name="keyset-encrypt-operation" className="grid gap-4" onSubmit={onSubmit}>
        <form.AppField name="signature" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Signature to verify"
              description="Label for the textarea field that contains the digital signature to be verified."
            />}
          >
            <field.Textarea rows={6}/>
          </field.Control>
        )} />

        <form.AppField name="plaintext" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Text to verify"
              description="Label for the textarea field that contains the plain text to verify if the digital signature is valid."
            />}
          >
            <field.Textarea rows={6}/>
          </field.Control>
        )} />

        <OperationResult valid={response?.valid} />

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="reset" variant="outline" onClick={onCancel}>
            <CancelLabel />
          </Button>
          <form.Submit>
            <KeysetVerifySignatureLabel />
          </form.Submit>
        </div>
      </form>
    </form.AppForm>
  );
}
