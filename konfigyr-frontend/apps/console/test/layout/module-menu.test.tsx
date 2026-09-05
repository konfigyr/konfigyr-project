import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('layout | <ModuleSidebarMenu/>', () => {
  afterEach(() => cleanup());

  test('should mark Namespace management as the active module on the namespace overview page', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Namespace management' })).toHaveAttribute('data-active', 'true');
    });
  });

  test('should mark Vault as the active module on the namespace services page', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr/services');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Vault' })).toHaveAttribute('data-active', 'true');
    });
  });

  test('should list all available modules with the PKI module marked as disabled', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Namespace management' })).toBeInTheDocument();
      expect(getByRole('link', { name: 'Vault' })).toBeInTheDocument();
      expect(getByRole('link', { name: 'Artifactory' })).toBeInTheDocument();
      expect(getByRole('link', { name: 'KMS' })).toBeInTheDocument();

      expect(getByRole('link', { name: 'PKI' })).toHaveAttribute('aria-disabled', 'true');
    });
  });

  test('should navigate to the selected module', async () => {
    const user = userEvents.setup();
    const { getByRole } = renderWithRouter('/namespace/konfigyr');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Vault' })).toBeInTheDocument();
    });

    await user.click(getByRole('link', { name: 'Vault' }));

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Vault' })).toBeInTheDocument();
    });
  });
});
