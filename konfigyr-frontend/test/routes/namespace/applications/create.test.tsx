import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | applications | create', () => {
  afterEach(() => cleanup());

  test('should render the type selector when no type is selected', async () => {
    const { findByRole } = renderWithRouter('/namespace/konfigyr/applications/create');
    const typeSelector = await findByRole('radiogroup');

    expect(within(typeSelector).getAllByRole('radio')).toHaveLength(3);
    expect(within(typeSelector).getByRole('radio', { name: /service account/i })).toBeInTheDocument();
    expect(within(typeSelector).getByRole('radio', { name: /ai agent/i })).toBeInTheDocument();
    expect(within(typeSelector).getByRole('radio', { name: /workload identity/i })).toBeInTheDocument();
  });

  test('should navigate to the form after selecting a type and clicking continue', async () => {
    const user = userEvent.setup();
    const { findByRole, getByRole, findByLabelText } = renderWithRouter('/namespace/konfigyr/applications/create');

    await user.click(await findByRole('radio', { name: /service account/i }));

    await user.click(
      getByRole('button', { name: /continue to application configuration/i }),
    );

    expect(await findByLabelText('Application name')).toBeInTheDocument();
  });
});
