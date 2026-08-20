import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor, within } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('components | layout | <ModuleSwitcher/>', () => {
  afterEach(() => cleanup());

  test('should show Home as the active module on the namespace overview page', async () => {
    const { container } = renderWithRouter('/namespace/konfigyr');

    await waitFor(() => {
      const header = container.querySelector('[data-slot="sidebar-header"]') as HTMLElement;
      expect(within(header).getByText('Home')).toBeInTheDocument();
    });
  });

  test('should show Vault as the active module on the namespace services page', async () => {
    const { container } = renderWithRouter('/namespace/konfigyr/services');

    await waitFor(() => {
      const header = container.querySelector('[data-slot="sidebar-header"]') as HTMLElement;
      expect(within(header).getByText('Vault')).toBeInTheDocument();
    });
  });

  test('should list all available modules with the PKI module marked as disabled', async () => {
    const user = userEvents.setup();
    const { getByRole } = renderWithRouter('/namespace/konfigyr');

    await waitFor(() => {
      expect(getByRole('button', { name: 'Switch domain' })).toBeInTheDocument();
    });

    await user.click(getByRole('button', { name: 'Switch domain' }));

    await waitFor(() => {
      expect(getByRole('menuitemradio', { name: 'Home' })).toBeInTheDocument();
      expect(getByRole('menuitemradio', { name: 'Vault' })).toBeInTheDocument();
      expect(getByRole('menuitemradio', { name: 'Artifactory' })).toBeInTheDocument();
      expect(getByRole('menuitemradio', { name: 'KMS' })).toBeInTheDocument();
      expect(getByRole('menuitemradio', { name: 'Administration' })).toBeInTheDocument();

      const pki = getByRole('menuitemradio', { name: /PKI/ });
      expect(pki).toHaveAttribute('aria-disabled', 'true');
      expect(within(pki).getByText('Coming soon')).toBeInTheDocument();
    });
  });

  test('should navigate to the selected module', async () => {
    const user = userEvents.setup();
    const { getByRole } = renderWithRouter('/namespace/konfigyr');

    await waitFor(() => {
      expect(getByRole('button', { name: 'Switch domain' })).toBeInTheDocument();
    });

    await user.click(getByRole('button', { name: 'Switch domain' }));

    await waitFor(() => {
      expect(getByRole('menuitemradio', { name: 'Vault' })).toBeInTheDocument();
    });

    await user.click(getByRole('menuitemradio', { name: 'Vault' }));

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Vault' })).toBeInTheDocument();
    });
  });
});
