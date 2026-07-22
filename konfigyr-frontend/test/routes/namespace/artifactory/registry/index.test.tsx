import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | registry', () => {
  afterEach(() => cleanup());

  test('should render the artifact registry list', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/artifactory/registry');

    await waitFor(() => {
      expect(getByText('com.example:public-service')).toBeInTheDocument();
      expect(getByText('com.example:private-service')).toBeInTheDocument();
    });
  });
});
