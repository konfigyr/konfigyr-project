import { afterAll, afterEach, beforeAll, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';
import { namespaces, profiles, propertyDescriptors, services } from '@konfigyr/test/helpers/mocks';
import { PropertyDialog } from '@konfigyr/components/vault/properties/property-dialog';

import type { RenderResult } from '@testing-library/react';
import type { ChangesetState, ConfigurationProperty, ServiceCatalog } from '@konfigyr/hooks/types';

const changeset: ChangesetState = {
  namespace: namespaces.konfigyr,
  service: services.konfigyrApi,
  profile: profiles.development,
  name: 'test changeset',
  state: 'DRAFT',
  properties: [{
    name: 'application.name',
    description: 'Application name property',
    typeName: 'java.lang.String',
    state: ConfigurationPropertyState.UPDATED,
    value: {
      encoded: 'konfigyr-frontend',
      decoded: 'konfigyr-frontend',
    },
    schema: {
      type: 'string',
    },
  }, {
    name: 'application.profile',
    description: 'Application profile property',
    typeName: 'java.lang.String',
    state: ConfigurationPropertyState.UNCHANGED,
    value: {
      encoded: 'staging',
      decoded: 'staging',
    },
    deprecation: {
      reason: 'This property is deprecated',
    },
    schema: {
      type: 'string',
      enum: ['staging', 'production'],
    },
  }],
  added: 0,
  modified: 1,
  deleted: 0,
};

const catalog: ServiceCatalog = {
  service: services.konfigyrApi,
  properties: [
    ...propertyDescriptors.springAopProperties,
    ...propertyDescriptors.springConfigProperties,
    ...propertyDescriptors.springLoggingProperties,
    ...propertyDescriptors.springSecurityProperties,
  ],
};

describe('components | vault | properties | <PropertyDialog/>', () => {
  let onAdd: (property: ConfigurationProperty<unknown>) => void;
  let result: RenderResult;

  beforeAll(() => {
    onAdd = vi.fn();
    result = renderWithQueryClient((
      <PropertyDialog changeset={changeset} catalog={catalog} onAdd={onAdd} />
    ));
  });

  afterEach(() => vi.clearAllMocks());

  afterAll(() => cleanup());

  test('should render the property dialog with the closed state', () => {
    expect(result.getByRole('button', { name: 'Add property' })).toBeInTheDocument();
    expect(result.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('should open the property dialog with the property search combobox in focus', async () => {
    await userEvents.click(result.getByRole('button', { name: 'Add property' }));

    expect(result.getByRole('dialog')).toBeInTheDocument();

    expect(result.getByRole('combobox', { name: 'Property name' })).toBeInTheDocument();
    expect(result.getByRole('combobox', { name: 'Property name' })).toHaveFocus();

    expect(result.queryByRole('textbox')).not.toBeInTheDocument();
  });

  test('should find a configuration property with simple string type', async () => {
    await userEvents.type(
      result.getByRole('combobox', { name: 'Property name' }),
      'spring.config.name',
    );

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(1);
    });

    expect(result.getByRole('option', { name: 'spring.config.name' })).toBeInTheDocument();

    await userEvents.keyboard('[Enter]');

    expect(result.getByRole('combobox', { name: 'Property name' })).toHaveValue('spring.config.name');
    expect(result.getByRole('textbox', { name: 'spring.config.name' })).toBeInTheDocument();
  });

  test('should add selected configuration property with the entered value', async () => {
    expect(result.getByRole('textbox', { name: 'spring.config.name' })).toHaveValue('application');

    await userEvents.clear(
      result.getByRole('textbox', { name: 'spring.config.name' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'spring.config.name' }),
      'Test configuration property',
    );

    await userEvents.keyboard('[Enter]');

    expect(onAdd).toHaveBeenCalledExactlyOnceWith({
      artifact: 'org.springframework.boot:spring-boot:4.0.3',
      name: 'spring.config.name',
      typeName: 'java.lang.String',
      schema: { type: 'string' },
      description: 'Config file name.',
      defaultValue: 'application',
      score: 100,
      state: ConfigurationPropertyState.ADDED,
      value: { encoded: 'Test configuration property', decoded: 'Test configuration property' },
    });

    expect(result.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('should add a configuration property with object schema type', async () => {
    await userEvents.click(result.getByRole('button', { name: 'Add property' }));

    await userEvents.type(
      result.getByRole('combobox', { name: 'Property name' }),
      'logging.level',
    );

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(1);
    });

    expect(result.getByRole('option', { name: 'logging.level' })).toBeInTheDocument();

    await userEvents.keyboard('[Tab]');

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(3);
    });

    expect(result.getByRole('option', { name: 'logging.level.root' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'logging.level.sql' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'logging.level.web' })).toBeInTheDocument();

    await userEvents.click(
      result.getByRole('option', { name: 'logging.level.web' }),
    );

    await userEvents.type(
      result.getByRole('combobox', { name: 'logging.level.web' }),
      'DEBUG',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Add property' }),
    );

    expect(onAdd).toHaveBeenCalledExactlyOnceWith({
      artifact: 'org.springframework.boot:spring-boot:4.0.3',
      name: 'logging.level.web',
      typeName: 'java.util.Map<java.lang.String,java.lang.String>',
      schema: {
        type: 'string',
        examples: ['debug', 'error', 'fatal', 'info', 'off', 'trace', 'warn'],
      },
      description: 'Log levels severity mapping. For instance, `logging.level.org.springframework=DEBUG`.',
      score: 101,
      state: ConfigurationPropertyState.ADDED,
      value: { encoded: 'DEBUG', decoded: 'DEBUG' },
    });

    expect(result.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('should add a configuration property with a complex object schema type', async () => {
    await userEvents.click(result.getByRole('button', { name: 'Add property' }));

    await userEvents.type(
      result.getByRole('combobox', { name: 'Property name' }),
      'spring.security.oauth2.client.registration',
    );

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(1);
    });

    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration' })).toBeInTheDocument();

    await userEvents.keyboard('[Tab]');

    // start typing to enter the object key...
    await userEvents.type(
      result.getByRole('combobox'),
      '.github',
    );

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(8);
    });

    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.provider' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.redirectUri' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.authorizationGrantType' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.clientAuthenticationMethod' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.clientSecret' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.clientId' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.clientSecret' })).toBeInTheDocument();
    expect(result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.scope' })).toBeInTheDocument();

    await userEvents.click(
      result.getByRole('option', { name: 'spring.security.oauth2.client.registration.github.clientSecret' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'spring.security.oauth2.client.registration.github.clientSecret' }),
      'client-secret',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Add property' }),
    );

    expect(onAdd).toHaveBeenCalledExactlyOnceWith({
      artifact: 'org.springframework.boot:spring-boot-security-oauth2-client:4.0.4',
      name: 'spring.security.oauth2.client.registration.github.clientSecret',
      typeName: 'java.util.Map<java.lang.String,org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties$Registration>',
      schema: { type: 'string' },
      description: 'OAuth client registrations.',
      score: 100,
      state: ConfigurationPropertyState.ADDED,
      value: { encoded: 'client-secret', decoded: 'client-secret' },
    });

    expect(result.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('should add a configuration property that has no matching descriptor', async () => {
    await userEvents.click(result.getByRole('button', { name: 'Add property' }));

    await userEvents.type(
      result.getByRole('combobox', { name: 'Property name' }),
      'konfigyr.test-missing-property',
    );

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(1);
    });

    expect(result.getByRole('option', { name: 'konfigyr.test-missing-property' })).toBeInTheDocument();

    await userEvents.keyboard('[Enter]');

    // start typing to enter the object key...
    await userEvents.type(
      result.getByRole('textbox', { name: 'konfigyr.test-missing-property' }),
      'Added value',
    );

    await userEvents.keyboard('[Enter]');

    expect(onAdd).toHaveBeenCalledExactlyOnceWith({
      name: 'konfigyr.test-missing-property',
      typeName: 'java.lang.String',
      schema: { type: 'string' },
      state: ConfigurationPropertyState.ADDED,
      value: { encoded: 'Added value', decoded: 'Added value' },
    });
  });

});
