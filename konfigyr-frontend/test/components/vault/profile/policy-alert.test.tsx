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

    const result = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(result.getByRole('alert')).toBeInTheDocument();
    expect(result.getByText('Profile is read-only')).toBeInTheDocument();
    expect(result.getByText(
      'No configuration changes may be applied to this profile. The existing configuration remains readable and auditable but cannot be modified.',
    )).toBeInTheDocument();
  });

  test('should render protected policy alert', () => {
    const profile: Profile = { ...profiles.development, policy: 'PROTECTED' };

    const result = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(result.getByRole('alert')).toBeInTheDocument();
    expect(result.getByText('Protected profile')).toBeInTheDocument();
    expect(result.getByText(
      'Changes to this profile require review and approval before being applied.',
    )).toBeInTheDocument();
  });

  test('should render unprotected policy alert', () => {
    const profile: Profile = { ...profiles.development, policy: 'UNPROTECTED' };

    const result = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(result.getByRole('alert')).toBeInTheDocument();
    expect(result.getByText('Unprotected profile')).toBeInTheDocument();
    expect(result.getByText(
      'Changes to this profile will be directly applied without any review or approval.',
    )).toBeInTheDocument();
  });
});
