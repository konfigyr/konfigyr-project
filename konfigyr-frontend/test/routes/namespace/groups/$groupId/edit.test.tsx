import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | groups | edit', () => {
  afterEach(() => cleanup());

  test('should render the existing claim in the edit form', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr/groups/io.github.acme/edit');

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Group Id' })).toHaveValue('io.github.acme');
      expect(getByRole('textbox', { name: 'Group Id' })).toBeDisabled();
      expect(getByRole('radio', { name: /source code/i })).toBeChecked();
    });
  });

  test('should update the group claim and redirect to the detail page', async () => {
    const user = userEvent.setup();
    const { getByRole, router } = renderWithRouter('/namespace/konfigyr/groups/io.github.acme/edit');

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Group Id' })).toHaveValue('io.github.acme');
    });

    await user.click(getByRole('button', { name: 'Save' }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr/groups/io.github.acme');
    });
  });
});
