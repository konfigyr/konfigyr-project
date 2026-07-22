import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | provision', () => {
  afterEach(() => cleanup());

  test('should render Namespace provisioning card', async () => {
    const { getByText, getByRole } = renderWithRouter('/namespace/provision');

    await waitFor(() => {
      expect(getByText('Tell us more about your organization or team that would be the owner of this namespace.'))
        .toBeInTheDocument();

      expect(getByRole('textbox', { name: 'Name' }))
        .toBeInTheDocument();

      expect(getByRole('textbox', { name: 'URL' }))
        .toBeInTheDocument();

      expect(getByRole('textbox', { name: 'Description' }))
        .toBeInTheDocument();
    });
  });

  test('should create namespace and redirect to namespace overview page', async () => {
    const user = userEvents.setup();
    const { getByRole, router } = renderWithRouter('/namespace/provision');

    await user.type(
      await waitFor(() => getByRole('textbox', { name: 'Name' })),
      'Available Namespace',
    );

    // wait for the debounced async slug validation to fully settle before submitting,
    // otherwise the form can reject the submission as invalid despite no visible errors.
    await new Promise(resolve => setTimeout(resolve, 300));

    await user.click(
      getByRole('button', { name: 'Create namespace' }),
    );

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/available-namespace');
    });
  });
});
