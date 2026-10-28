import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { DatabaseBackup } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Button } from '@konfigyr/ui/components/button';
import { Field, FieldDescription, FieldLabel } from '@konfigyr/ui/components/field';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/ui/components/select';
import { ErrorState } from '@konfigyr/components/error';
import { useNamespace } from '@konfigyr/hooks/namespace/context';
import { useNamespaceServicesQuery } from '@konfigyr/hooks/namespace/query';
import { getProfilePropertiesQuery } from '@konfigyr/hooks/vault/changeset';
import { buildConfigurationProperty } from '@konfigyr/hooks/vault/config-file-parser';
import { useGetProfiles } from '@konfigyr/hooks/vault/profiles';
import type { Service } from '@konfigyr/hooks/namespace/types';
import type { PropertiesImporter } from '@konfigyr/hooks/vault/properties-importer';
import type { Profile } from '@konfigyr/hooks/types';

/**
 * Renders the select with the services that belong to the current namespace.
 */
function ServiceSelect ({ value, onChange }: {
  value: Service | null,
  onChange: (service: Service | null) => void,
}) {
  const namespace = useNamespace();
  const { data: services = [], isPending, error } = useNamespaceServicesQuery(namespace.slug);

  if (error) {
    return <ErrorState error={error} />;
  }

  return (
    <Field>
      <FieldLabel>
        <FormattedMessage
          defaultMessage="Application"
          description="Label for the application select in the profile configuration importer."
        />
      </FieldLabel>
      <Select
        name="service"
        value={value?.slug ?? null}
        disabled={isPending}
        onValueChange={(slug) => onChange(services.find(it => it.slug === slug) ?? null)}
      >
        <SelectTrigger className="w-full">
          <SelectValue>
            {value?.name ?? (
              <FormattedMessage
                defaultMessage="Select an application"
                description="Placeholder for the application select in the profile configuration importer."
              />
            )}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            {services.map((service) => (
              <SelectItem key={service.slug} value={service.slug}>
                {service.name}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
    </Field>
  );
}

/**
 * Renders the profile select for the selected service. The profiles are loaded only once
 * the service is known, which is why this select is a separate component.
 */
function ProfileSelect ({ service, value, exclude, onChange }: {
  service: Service,
  value: Profile | null,
  exclude?: Profile,
  onChange: (profile: Profile | null) => void,
}) {
  const namespace = useNamespace();
  const { data, isPending, error } = useGetProfiles(namespace, service);
  const profiles = (data ?? []).filter(it => it.id !== exclude?.id);

  if (error) {
    return <ErrorState error={error} />;
  }

  return (
    <Field>
      <FieldLabel>
        <FormattedMessage
          defaultMessage="Profile"
          description="Label for the profile select in the profile configuration importer."
        />
      </FieldLabel>
      <Select
        name="profile"
        value={value?.slug ?? null}
        disabled={isPending}
        onValueChange={(slug) => onChange(profiles.find(it => it.slug === slug) ?? null)}
      >
        <SelectTrigger className="w-full">
          <SelectValue>
            {value?.name ?? (
              <FormattedMessage
                defaultMessage="Select a profile"
                description="Placeholder for the profile select in the profile configuration importer."
              />
            )}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            {profiles.map((profile) => (
              <SelectItem key={profile.slug} value={profile.slug}>
                {profile.name}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
    </Field>
  );
}

/**
 * Renders the importer that copies the configuration properties of another service profile.
 */
export function ProfileConfigurationImporter ({ importer, currentService, currentProfile }: {
  importer: PropertiesImporter,
  currentService: Service,
  currentProfile: Profile,
}) {
  const namespace = useNamespace();
  const client = useQueryClient();
  const [service, setService] = useState<Service | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);

  function onServiceChange (selected: Service | null) {
    setService(selected);
    setProfile(null);
  }

  async function onImport () {
    if (!service || !profile) {
      return;
    }

    await importer(async () => {
      const values = await client.fetchQuery(getProfilePropertiesQuery(namespace, service, profile));

      return Object.entries(values).map(([name, value]) => buildConfigurationProperty(name, value));
    });
  }

  return (
    <div className="space-y-4">
      <ServiceSelect value={service} onChange={onServiceChange} />

      {service ? (
        <ProfileSelect
          service={service}
          value={profile}
          exclude={service.id === currentService.id ? currentProfile : undefined}
          onChange={setProfile}
        />
      ) : (
        <FieldDescription>
          <FormattedMessage
            defaultMessage="Select the application that owns the profile you want to import first."
            description="Hint shown in the profile configuration importer until an application is selected."
          />
        </FieldDescription>
      )}

      <Button disabled={!service || !profile} onClick={onImport}>
        <DatabaseBackup/>
        <FormattedMessage
          defaultMessage="Load profile"
          description="Button label for loading the configuration properties of the selected application profile."
        />
      </Button>
    </div>
  );
}
