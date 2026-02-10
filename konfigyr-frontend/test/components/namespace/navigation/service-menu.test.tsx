import { afterEach, describe, expect, test } from 'vitest';
import { Layout } from '@konfigyr/components/layout';
import { ServiceNavigationMenu } from '@konfigyr/components/namespace/navigation/service-menu';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces, services } from '@konfigyr/test/helpers/mocks';
import { cleanup } from '@testing-library/react';

describe('components | namespace | navigation | <ServiceNavigationMenu/>', () => {
  afterEach(() => cleanup());

  test('should render services navigation menu', () => {
    const result = renderComponentWithRouter(
      <Layout>
        <ServiceNavigationMenu namespace={namespaces.konfigyr} service={services.konfigyrApi} />
      </Layout>,
    );

    expect(result.getByRole('link', { name: 'Overview' })).toBeInTheDocument();
    expect(result.getByRole('link', { name: 'Overview' }).getAttribute('href'))
      .toBe('/namespace/konfigyr/services/konfigyr-api');

    expect(result.getByRole('link', { name: 'Change requests' })).toBeInTheDocument();
    expect(result.getByRole('link', { name: 'Change requests' }).getAttribute('href'))
      .toBe('/namespace/konfigyr/services/konfigyr-api/requests');

    expect(result.getByRole('link', { name: 'Settings' })).toBeInTheDocument();
    expect(result.getByRole('link', { name: 'Settings' }).getAttribute('href'))
      .toBe('/namespace/konfigyr/services/konfigyr-api/settings');
  });

});
