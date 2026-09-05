import { FormattedMessage } from 'react-intl';
import { MonitorCloudIcon } from 'lucide-react';
import { useErrorNotification } from '@konfigyr/components/error';
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { ApplicationTypeSelector } from './application-type-selector';

import type { NamespaceApplicationType } from '@konfigyr/hooks/types';

export function ApplicationTypeForm({ onSubmit }: { onSubmit: (type: NamespaceApplicationType) => void | Promise<void> }) {
  const errorNotification = useErrorNotification();
  const form = useForm({
    defaultValues: { type: 'SERVICE_ACCOUNT' },
    onSubmit: async ({ value }) => {
      try {
        await onSubmit(value.type as NamespaceApplicationType);
      } catch (error) {
        errorNotification(error);
      }
    },
  });

  return (
    <form.AppForm>
      <form name="application-type-form" onSubmit={useFormSubmit(form)}>
        <Card className="border">
          <CardHeader>
            <CardTitle className="flex flex-col items-center gap-6 my-2">
              <MonitorCloudIcon size={64} strokeWidth="1" />
              <p className="text-center text-lg lg:text-xl">
                <FormattedMessage
                  defaultMessage="What type of application are you registering?"
                  description="Heading on the application type selection step"
                />
              </p>
            </CardTitle>
            <CardDescription className="text-center">
              <FormattedMessage
                defaultMessage="Each type is purpose-built for a specific use case and authentication model, pick the one that best describes what this application will do. The type can't be changed after creation."
                description="Subtext on the application type selection step"
              />
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form.AppField name="type" children={(field) => (
              <ApplicationTypeSelector
                value={field.state.value as NamespaceApplicationType}
                onChange={field.handleChange}
              />
            )} />
          </CardContent>
          <CardFooter>
            <CardAction>
              <form.Submit>
                <FormattedMessage
                  defaultMessage="Continue to application configuration"
                  description="Namespace application type selector form submit button label."
                />
              </form.Submit>
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
