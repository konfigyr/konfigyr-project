import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | manifest', () => {
  afterEach(() => cleanup());

  test('should render service manifest page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/manifest');

    await waitFor(() => {
      expect(getByText('Service artifacts')).toBeInTheDocument();
      expect(getByText('com.konfigyr:konfigyr-crypto-jdbc')).toBeInTheDocument();
      expect(getByText('org.springframework.boot:spring-boot')).toBeInTheDocument();
      expect(getByText('org.springframework.boot:spring-boot-autoconfigure')).toBeInTheDocument();
    });
  });

  test('should render an empty service manifest page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-id/manifest');

    await waitFor(() => {
      expect(getByText('Service artifacts')).toBeInTheDocument();
      expect(getByText('No artifacts found')).toBeInTheDocument();
    });
  });

});
