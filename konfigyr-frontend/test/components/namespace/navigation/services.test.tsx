import { afterEach, describe, expect, test } from 'vitest';
import { Layout } from '@konfigyr/layout';
import { NamespaceServicesNavigationMenu } from '@konfigyr/components/namespace/navigation/services';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

describe('components | namespace | navigation | <NamespaceServicesNavigationMenu/>', () => {
  afterEach(() => cleanup());

  test('should render namespace services navigation menu', async () => {
    const result = renderComponentWithRouter(
      <Layout>
        <NamespaceServicesNavigationMenu namespace={namespaces.konfigyr} />
      </Layout>,
    );

    expect(result.getByRole('link', { name: 'Overview' })).toBeInTheDocument();
    expect(result.getByText('Services')).toBeInTheDocument();
    expect(result.getByText('Loading services...')).toBeInTheDocument();

    await waitFor(() => {
      expect(result.getByRole('button', { name: 'konfigyr-api' })).toBeInTheDocument();
      expect(result.getByRole('button', { name: 'konfigyr-id' })).toBeInTheDocument();
    });
  });

  test('should render namespace services navigation menu with empty state', async () => {
    const result = renderComponentWithRouter(
      <Layout>
        <NamespaceServicesNavigationMenu namespace={namespaces.johnDoe} />
      </Layout>,
    );

    await waitFor(() => {
      expect(result.getByText('No services found')).toBeInTheDocument();
    });

    expect(result.getByRole('button', { name: 'Create new service' })).toBeInTheDocument();
  });

  test('should render namespace services navigation menu with an error state', async () => {
    const result = renderComponentWithRouter(
      <Layout>
        <NamespaceServicesNavigationMenu namespace={namespaces.unknown} />
      </Layout>,
    );

    await waitFor(() => {
      expect(result.getByText('Namespace not found')).toBeInTheDocument();
    });
  });

  test('should open the create service dialog', async () => {
    const user = userEvents.setup();
    const result = renderComponentWithRouter(
      <Layout>
        <NamespaceServicesNavigationMenu namespace={namespaces.johnDoe} />
      </Layout>,
    );

    expect(result.queryByRole('dialog')).not.toBeInTheDocument();

    const trigger = result.getByRole('button', { name: 'Create new service' });
    expect(trigger).toBeInTheDocument();

    await user.click(trigger);

    await waitFor(() => {
      expect(result.getByRole('dialog')).toBeInTheDocument();
      expect(result.getByRole('dialog')).toHaveAccessibleName('Create new service');
      expect(result.getByRole('dialog')).toHaveAccessibleDescription(
        'Register your Spring Boot service within this namespace to begin managing its environment-specific configurations.',
      );

      expect(result.getByRole('textbox', { name: 'Identifier' })).toBeInTheDocument();
      expect(result.getByRole('textbox', { name: 'Display name' })).toBeInTheDocument();
      expect(result.getByRole('textbox', { name: 'Description' })).toBeInTheDocument();
    });
  });
});
