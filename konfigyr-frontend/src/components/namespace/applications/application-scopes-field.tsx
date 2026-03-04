import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { useNamespaceScopes } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { Checkbox } from '@konfigyr/components/ui/checkbox';
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from '@konfigyr/components/ui/field';

import type { Namespace } from '@konfigyr/hooks/types';

export function ApplicationScopesField({ namespace, value, onChange }: {
  namespace: Namespace,
  value: Array<string>,
  onChange: (value: Array<string>) => void
}) {
  const { data: scopes, error, isLoading, isError } = useNamespaceScopes(namespace.slug);

  const onCheck = useCallback((scope: string, checked: boolean) => {
    if (checked) {
      onChange([...value, scope]);
    } else {
      onChange(value.filter((s) => s !== scope));
    }
  }, [value, onChange]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center my-4">
        <FormattedMessage
          defaultMessage="Loading scopes, pleas be patient..."
          description="Loading message for the application scopes field"
        />
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState error={error} />
    );
  }

  return (
    <FieldSet>
      <FieldLegend variant="label">
        <FormattedMessage
          defaultMessage="Select scopes"
          description="Label for the application scopes field"
        />
      </FieldLegend>
      <FieldDescription>
        <FormattedMessage
          defaultMessage="Select which permissions this application is allowed to perform. These scopes define which Konfigyr API operations the client can access."
          description="Help text for the namespace application scopes field"
        />
      </FieldDescription>
      <FieldGroup className="gap-3">
        {scopes?.map((scope) => (
          <Field key={scope.name} orientation="horizontal">
            <Checkbox
              id={scope.name}
              name={scope.name}
              checked={value.includes(scope.name)}
              onCheckedChange={checked => onCheck(scope.name, !!checked)}
            />
            <FieldContent>
              <FieldLabel
                htmlFor={scope.name}
                className="font-semibold font-mono"
              >
                {scope.name}
              </FieldLabel>
              {scope.description && (
                <FieldDescription>
                  {scope.description}
                </FieldDescription>
              )}
            </FieldContent>
          </Field>
        ))}
      </FieldGroup>
    </FieldSet>
  );
}
