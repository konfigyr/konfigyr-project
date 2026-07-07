import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { DATE_FORMAT_OPTIONS, GroupVerification } from '@konfigyr/components/groups/group-verification-challenge';

import type { GroupVerification as GroupVerificationType, VerificationChallenge } from '@konfigyr/hooks/types';

const dateFormat = new Intl.DateTimeFormat('en', DATE_FORMAT_OPTIONS );

describe('components | groups | <GroupVerification/>', () => {
  afterEach(() => cleanup());

  test('should render the verification details card', () => {
    const verification: GroupVerificationType = {
      id: 'verification-1',
      groupId: 'com.example.group',
      state: 'PENDING',
      createdAt: '2026-07-07T12:00:00Z',
      verifiedAt: '2026-07-08T12:00:00Z',
      revokedAt: '2026-07-09T12:00:00Z',
    };

    const challenge: VerificationChallenge = {
      id: 'challenge-1',
      verificationId: 'verification-1',
      method: 'DNS',
      token: 'token-123',
      state: 'UNVERIFIED',
      createdAt: '2026-07-07T12:00:00Z',
      verifiedAt: null,
      expiresAt: null,
    };

    const { getByText } = renderWithMessageProvider(
      <GroupVerification verification={verification} challenge={challenge} />,
    );

    expect(getByText('Group verification details')).toBeInTheDocument();

    expect(getByText('Group Id')).toBeInTheDocument();
    expect(getByText('com.example.group')).toBeInTheDocument();

    expect(getByText('Verification state')).toBeInTheDocument();
    expect(getByText('Pending')).toBeInTheDocument();

    expect(getByText('Method')).toBeInTheDocument();
    expect(getByText('DNS')).toBeInTheDocument();

    expect(getByText('Challenge state')).toBeInTheDocument();
    expect(getByText('Unverified')).toBeInTheDocument();

    expect(getByText('Created at')).toBeInTheDocument();
    expect(getByText(dateFormat.format(new Date(verification.createdAt)))).toBeInTheDocument();

    expect(getByText('Verified at')).toBeInTheDocument();
    expect(getByText(dateFormat.format(new Date(verification.verifiedAt as string)))).toBeInTheDocument();

    expect(getByText('Revoked at')).toBeInTheDocument();
    expect(getByText(dateFormat.format(new Date(verification.revokedAt as string)))).toBeInTheDocument();
  });

  test('should render dns activation instructions for a pending verification', () => {
    const verification: GroupVerificationType = {
      id: 'verification-2',
      groupId: 'com.example.group',
      state: 'PENDING',
      createdAt: '2026-07-07T12:00:00Z',
      verifiedAt: null,
      revokedAt: null,
    };

    const challenge: VerificationChallenge = {
      id: 'challenge-2',
      verificationId: 'verification-2',
      method: 'DNS',
      token: 'token-456',
      state: 'UNVERIFIED',
      createdAt: '2026-07-07T12:00:00Z',
      verifiedAt: null,
      expiresAt: null,
    };

    const { getByText } = renderWithMessageProvider(
      <GroupVerification verification={verification} challenge={challenge} />,
    );

    expect(getByText('Add a DNS TXT record')).toBeInTheDocument();
    expect(getByText('Follow the steps below to prove ownership of com.example.group.')).toBeInTheDocument();
    expect(getByText('konfigyr-verification=token-456')).toBeInTheDocument();
  });

  test('should render source code activation instructions for a pending verification', () => {
    const verification: GroupVerificationType = {
      id: 'verification-3',
      groupId: 'com.github.acme',
      state: 'PENDING',
      createdAt: '2026-07-07T12:00:00Z',
      verifiedAt: null,
      revokedAt: null,
    };

    const challenge: VerificationChallenge = {
      id: 'challenge-3',
      verificationId: 'verification-3',
      method: 'SOURCE_CODE',
      token: 'token-789',
      state: 'UNVERIFIED',
      createdAt: '2026-07-07T12:00:00Z',
      verifiedAt: null,
      expiresAt: null,
    };

    const { getByText } = renderWithMessageProvider(
      <GroupVerification verification={verification} challenge={challenge} />,
    );

    expect(getByText('Create a public repository on GitHub')).toBeInTheDocument();
    expect(getByText('Follow the steps below to prove ownership of the acme account on GitHub.')).toBeInTheDocument();
    expect(getByText('KFGYR-token-789')).toBeInTheDocument();
  });

  test('should not render activation instructions for a revoked verification', () => {
    const verification: GroupVerificationType = {
      id: 'verification-4',
      groupId: 'com.example.group',
      state: 'REVOKED',
      createdAt: '2026-07-07T12:00:00Z',
      verifiedAt: '2026-07-08T12:00:00Z',
      revokedAt: '2026-07-09T12:00:00Z',
    };

    const challenge: VerificationChallenge = {
      id: 'challenge-4',
      verificationId: 'verification-4',
      method: 'DNS',
      token: 'token-000',
      state: 'EXPIRED',
      createdAt: '2026-07-07T12:00:00Z',
      verifiedAt: null,
      expiresAt: null,
    };

    const { queryByText } = renderWithMessageProvider(
      <GroupVerification verification={verification} challenge={challenge} />,
    );

    expect(queryByText('Add a DNS TXT record')).not.toBeInTheDocument();
    expect(queryByText('Create a public repository on GitHub')).not.toBeInTheDocument();
  });
});
