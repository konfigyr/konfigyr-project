import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | profile | history', () => {
  afterEach(() => cleanup());

  test('should render profile history page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/profiles/development/history');

    await waitFor(() => {
      expect(getByText('9eadce4691d8fcd863aeeb07ef81d8146083d814')).toBeInTheDocument();
      expect(getByText('Changeset draft')).toBeInTheDocument();
      expect(getByText('Test User <test.user@ebf.com>')).toBeInTheDocument();
    });
  });

});
