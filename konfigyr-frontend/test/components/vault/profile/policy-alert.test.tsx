import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { PolicyAlert } from '@konfigyr/components/vault/profile/policy-alert';
import { profiles } from '@konfigyr/test/helpers/mocks';

import type { Profile } from '@konfigyr/hooks/types';

describe('components | vault | profile | <PolicyAlert/>', () => {
  afterEach(() => cleanup());

  test('should render locked policy alert', () => {
    const profile: Profile = { ...profiles.development, policy: 'LOCKED' };

    const result = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(result.getByRole('alert')).toBeInTheDocument();
    expect(result.getByText('Profile is locked')).toBeInTheDocument();
    expect(result.getByText('This profile is locked and cannot be edited.')).toBeInTheDocument();
  });

  test('should render protected policy alert', () => {
    const profile: Profile = { ...profiles.development, policy: 'PROTECTED' };

    const result = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(result.getByRole('alert')).toBeInTheDocument();
    expect(result.getByText('Protected profile')).toBeInTheDocument();
    expect(result.getByText('Changes to this profile require review and approval before being applied.')).toBeInTheDocument();
  });

  test('should render unprotected policy alert', () => {
    const profile: Profile = { ...profiles.development, policy: 'UNPROTECTED' };

    const result = renderWithMessageProvider(
      <PolicyAlert profile={profile} />,
    );

    expect(result.getByRole('alert')).toBeInTheDocument();
    expect(result.getByText('Unprotected profile')).toBeInTheDocument();
    expect(result.getByText('Changes to this profile will be directly applied without any review or approval.')).toBeInTheDocument();
  });
});
