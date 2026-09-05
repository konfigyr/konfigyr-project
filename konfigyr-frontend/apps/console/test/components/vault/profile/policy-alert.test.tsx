import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { PolicyAlert } from '@konfigyr/components/vault/profile/policy-alert';
import { profiles } from '@konfigyr/test/helpers/mocks';

import type { Profile } from '@konfigyr/hooks/types';

describe('components | vault | profile | <PolicyAlert/>', () => {
  afterEach(() => cleanup());

  test('should render immutable policy alert', () => {
    const profile: Profile = { ...profiles.development, policy: 'IMMUTABLE' };

    const { getByRole } = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Profile is read-only');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'No configuration changes may be applied to this profile. The existing configuration remains readable and auditable but cannot be modified.',
    );
  });

  test('should render protected policy alert', () => {
    const profile: Profile = { ...profiles.development, policy: 'PROTECTED' };

    const { getByRole } = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Protected profile');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'Changes to this profile require review and approval before being applied.',
    );
  });

  test('should render unprotected policy alert', () => {
    const profile: Profile = { ...profiles.development, policy: 'UNPROTECTED' };

    const { getByRole } = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Unprotected profile');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'Changes to this profile will be directly applied without any review or approval.',
    );
  });
});
