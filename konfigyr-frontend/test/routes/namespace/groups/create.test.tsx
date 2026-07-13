import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | groups | create', () => {
  afterEach(() => cleanup());

  test('should render the claim form', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr/groups/create');

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Group Id' })).toBeInTheDocument();
      expect(getByRole('radio', { name: /dns/i })).toBeChecked();
      expect(getByRole('radio', { name: /source code/i })).toBeInTheDocument();
    });
  });

  test('should create a group claim and redirect to the detail page', async () => {
    const user = userEvent.setup();
    const { getByRole, router, findByLabelText } = renderWithRouter('/namespace/konfigyr/groups/create');

    await user.type(await findByLabelText('Group Id'), 'io.github.acme');
    await user.click(getByRole('radio', { name: /source code/i }));
    await user.click(getByRole('button', { name: 'Save' }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr/groups/io.github.acme');
    });
  });
});
