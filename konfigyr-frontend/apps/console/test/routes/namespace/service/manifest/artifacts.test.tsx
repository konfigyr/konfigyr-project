import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | manifest | artifacts', () => {
  afterEach(() => cleanup());

  test('should render service manifest artifacts page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/manifest/artifacts');

    await waitFor(() => {
      expect(getByText('com.konfigyr:konfigyr-crypto-jdbc')).toBeInTheDocument();
      expect(getByText('org.springframework.boot:spring-boot')).toBeInTheDocument();
      expect(getByText('org.springframework.boot:spring-boot-autoconfigure')).toBeInTheDocument();
    });
  });

  test('should render an empty service manifest artifacts page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-id/manifest/artifacts');

    await waitFor(() => {
      expect(getByText('No artifacts found')).toBeInTheDocument();
    });
  });

});
