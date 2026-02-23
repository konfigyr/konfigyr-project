import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';
import { ProfileMenu } from '@konfigyr/components/vault/navigation/profile-menu';

describe('components | vault | navigation | <ProfileMenu/>', () => {
  afterEach(() => cleanup());

  test('should render profile menu', () => {
    const result = renderComponentWithRouter(
      <ProfileMenu
        namespace={namespaces.konfigyr}
        service={services.konfigyrId}
        profiles={[profiles.staging, profiles.development]}
      />,
    );

    expect(result.getByRole('tablist')).toBeInTheDocument();
    expect(result.getByRole('link')).toBeInTheDocument();
    expect(result.getAllByRole('tab')).length(2);

    expect(result.getByRole('tab', { name: profiles.development.name }));
    expect(result.getByRole('tab', { name: profiles.staging.name }));
  });
});
