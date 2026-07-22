import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { GroupVerificationDetails } from '@konfigyr/components/artifactory/groups/group-verification-details';

import type { GroupVerification, Namespace, VerificationChallenge } from '@konfigyr/hooks/types';

const revokeGroupVerification = vi.hoisted(() => vi.fn());
const verifyGroupVerification = vi.hoisted(() => vi.fn());
const errorNotification = vi.hoisted(() => vi.fn());

vi.mock('@konfigyr/hooks', () => ({
  useRevokeGroupVerification: () => ({ isPending: false, mutateAsync: revokeGroupVerification }),
  useVerifyGroupVerification: () => ({ isPending: false, mutateAsync: verifyGroupVerification }),
}));

vi.mock('@konfigyr/components/error', () => ({
  useErrorNotification: () => errorNotification,
}));

const namespace: Namespace = {
  id: '2',
  slug: 'konfigyr',
  name: 'Konfigyr',
  description: 'Konfigyr namespace',
};

const activeVerification: GroupVerification = {
  id: 'verification-active',
  groupId: 'com.example.group',
  state: 'ACTIVE',
  createdAt: '2026-07-07T00:00:00Z',
  verifiedAt: '2026-07-08T00:00:00Z',
  revokedAt: null,
};

const pendingVerification: GroupVerification = {
  id: 'verification-pending',
  groupId: 'io.github.acme',
  state: 'PENDING',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: null,
  revokedAt: null,
};

const pendingChallenge: VerificationChallenge = {
  id: 'challenge-pending',
  verificationId: pendingVerification.id,
  method: 'SOURCE_CODE',
  token: 'token-123',
  state: 'UNVERIFIED',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: null,
  expiresAt: null,
};

const conflictedVerification: GroupVerification = {
  id: 'verification-conflicted',
  groupId: 'com.acme.widgets',
  state: 'PENDING',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: null,
  revokedAt: null,
  conflictingOwners: ['ebf'],
};

const multiConflictedVerification: GroupVerification = {
  ...conflictedVerification,
  id: 'verification-multi-conflicted',
  groupId: 'com.acme.multi',
  conflictingOwners: ['ebf', 'acme-legacy'],
};

const failedVerification: GroupVerification = {
  id: 'verification-failed',
  groupId: 'com.acme.failed',
  state: 'FAILED',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: null,
  revokedAt: null,
};

const revokedVerification: GroupVerification = {
  id: 'verification-revoked',
  groupId: 'com.acme.revoked',
  state: 'REVOKED',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: '2026-07-09T00:00:00Z',
  revokedAt: '2026-07-10T00:00:00Z',
};

describe('components | groups | <GroupVerificationDetails/>', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render an active group claim', () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <GroupVerificationDetails namespace={namespace} verification={activeVerification} challenges={[]}/>,
    );

    expect(getByRole('heading', { name: 'com.example.group' })).toBeInTheDocument();
    expect(getByText('Ownership verified')).toBeInTheDocument();
    expect(getByText('Group verification details')).toBeInTheDocument();
    expect(getByText('Active')).toBeInTheDocument();
    expect(getByRole('button', { name: 'Revoke claim' })).toBeInTheDocument();
  });

  test('should render a pending group claim with a source-code challenge', () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <GroupVerificationDetails namespace={namespace} verification={pendingVerification} challenges={[pendingChallenge]}/>,
    );

    expect(getByRole('heading', { name: 'io.github.acme' })).toBeInTheDocument();
    expect(getByText('Claim pending')).toBeInTheDocument();
    expect(getByText('Create the marker repository for io.github.acme on GitHub, then re-check this claim.')).toBeInTheDocument();
    expect(getByText('Pending')).toBeInTheDocument();
    expect(getByRole('button', { name: 'Cancel claim' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Verify claim' })).toBeInTheDocument();
  });

  test('should not render a conflict banner when there are no conflicting owners', () => {
    const { getByText, queryByText } = renderComponentWithRouter(
      <GroupVerificationDetails namespace={namespace} verification={activeVerification} challenges={[]}/>,
    );

    expect(getByText('Ownership verified')).toBeInTheDocument();
    expect(queryByText('Other namespaces already own artifacts here')).not.toBeInTheDocument();
  });

  test('should surface conflicting owners', () => {
    const { getAllByRole, getByText } = renderComponentWithRouter(
      <GroupVerificationDetails namespace={namespace} verification={conflictedVerification} challenges={[]}/>,
    );

    expect(getByText('Other namespaces already own artifacts here')).toBeInTheDocument();
    expect(getByText(
      'Namespace ebf already publishes artifacts under this groupId. You may need to request an ownership transfer before you can publish.',
    )).toBeInTheDocument();
    expect(getAllByRole('alert')).toHaveLength(2);
  });

  test('should link to a pre-filled transfer request when there is a single conflicting owner', () => {
    const { getByRole } = renderComponentWithRouter(
      <GroupVerificationDetails namespace={namespace} verification={conflictedVerification} challenges={[]}/>,
    );

    expect(getByRole('link', { name: 'Request transfer' })).toHaveAttribute(
      'href',
      '/namespace/konfigyr/artifactory/transfers/create?groupId=com.acme.widgets&fromNamespace=ebf',
    );
  });

  test('should link to a transfer request without a pre-filled namespace when there are multiple conflicting owners', () => {
    const { getByRole } = renderComponentWithRouter(
      <GroupVerificationDetails namespace={namespace} verification={multiConflictedVerification} challenges={[]}/>,
    );

    expect(getByRole('link', { name: 'Request transfer' })).toHaveAttribute(
      'href',
      '/namespace/konfigyr/artifactory/transfers/create?groupId=com.acme.multi',
    );
  });

  test('should only show Verify claim for a failed group claim', () => {
    const { getByRole, queryByRole } = renderComponentWithRouter(
      <GroupVerificationDetails namespace={namespace} verification={failedVerification} challenges={[]}/>,
    );

    expect(getByRole('button', { name: 'Verify claim' })).toBeInTheDocument();
    expect(queryByRole('button', { name: 'Revoke claim' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Cancel claim' })).not.toBeInTheDocument();
  });

  test('should only show Claim again for a revoked group claim', () => {
    const { getByRole, queryByRole } = renderComponentWithRouter(
      <GroupVerificationDetails namespace={namespace} verification={revokedVerification} challenges={[]}/>,
    );

    expect(getByRole('link', { name: 'Claim again' })).toBeInTheDocument();
    expect(queryByRole('button', { name: 'Revoke claim' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Cancel claim' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Verify claim' })).not.toBeInTheDocument();
  });
});
