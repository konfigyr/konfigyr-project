import { DatabaseBackup } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useForm, useFormSubmit } from '@konfigyr/ui/components/form';
import { FetchConfigSchema, fetchSpringConfigHandler } from '@konfigyr/hooks/vault/-handler';
import { jsonToProperties } from '@konfigyr/hooks/vault/config-file-parser';
import type { FetchConfigRequest } from '@konfigyr/hooks/vault/-handler';
import type { PropertiesImporter } from '@konfigyr/hooks/vault/properties-importer';
import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

/**
 * Parses Spring Cloud Config server response and converts effective `propertySources[*].source` entries into flat configuration properties.
 *
 * @param payload - Raw JSON payload returned by Spring Cloud Config server
 */
const parseSpringCloudConfigResponse = (payload: unknown): Array<ConfigurationProperty<string>> => {
  if (!payload || typeof payload !== 'object') {
    throw new Error('Invalid configuration response');
  }

  const propertySources = (payload as { propertySources?: unknown }).propertySources;
  if (!Array.isArray(propertySources)) {
    throw new Error('Invalid configuration response: missing propertySources');
  }

  const mergedSource: Record<string, unknown> = {};

  // Spring Cloud returns sources in precedence order (highest first).
  // Merge from last to first so higher-precedence sources override lower ones.
  for (let i = propertySources.length - 1; i >= 0; i--) {
    const source = (propertySources[i] as { source?: unknown }).source;
    if (source && typeof source === 'object' && !Array.isArray(source)) {
      Object.assign(mergedSource, source);
    }
  }

  return jsonToProperties(mergedSource);
};

/**
 * Fetches the configuration from the Spring Cloud Config server and converts it into configuration properties.
 *
 * @param payload - Config server URL and credentials
 */
const fetchSpringCloudConfig = async (payload: FetchConfigRequest): Promise<Array<ConfigurationProperty<string>>> => {
  const response = await fetchSpringConfigHandler({ data: payload });
  return parseSpringCloudConfigResponse(response);
};

/**
 * Renders the Spring Cloud Config importer form.
 */
export function SpringCloudConfigurationImporter ({ importer }: {
  importer: PropertiesImporter,
}) {
  const form = useForm({
    defaultValues: {
      username: '',
      password: '',
      configServerUrl: '',
    },
    validators: {
      onSubmit: FetchConfigSchema,
    },
    onSubmit: async ({ value }) => {
      await importer(() => fetchSpringCloudConfig({
        username: value.username.trim(),
        password: value.password.trim(),
        configServerUrl: value.configServerUrl.trim(),
      }));
    },
  });

  const onSubmit = useFormSubmit(form);

  return (
    <form.AppForm>
      <form onSubmit={onSubmit} className="space-y-4">
        <form.AppField
          name="username"
          children={(field) => (
            <field.Control
              label={(
                <FormattedMessage
                  defaultMessage="User name"
                  description="Label for username input in Spring Cloud configuration importer."
                />
              )}
              render={<field.Input type="text" autoComplete="username"/>}
            />
          )}
        />

        <form.AppField
          name="password"
          children={(field) => (
            <field.Control
              label={(
                <FormattedMessage
                  defaultMessage="Password"
                  description="Label for password input in Spring Cloud configuration importer."
                />
              )}
              render={<field.Input type="password" autoComplete="current-password"/>}
            />
          )}
        />

        <form.AppField
          name="configServerUrl"
          children={(field) => (
            <field.Control
              label={(
                <FormattedMessage
                  defaultMessage="Config server URL"
                  description="Label for config server URL input in Spring Cloud configuration importer."
                />
              )}
              render={<field.Input type="text" placeholder="https://config.com/api/configs/app/profile/label"/>}
            />
          )}
        />

        <form.Submit variant="default">
          <DatabaseBackup/>
          <FormattedMessage
            defaultMessage="Fetch config"
            description="Button label for fetching configuration from Spring Cloud Config server."
          />
        </form.Submit>
      </form>
    </form.AppForm>
  );
}
