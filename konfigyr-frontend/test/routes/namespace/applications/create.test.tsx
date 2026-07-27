import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, within } from '@testing-library/react';
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
});
