import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { AccountContext, membershipKeys } from '@konfigyr/hooks';
import { createTestQueryClient, renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { accounts, artifacts, namespaces } from '@konfigyr/test/helpers/mocks';
import { ArtifactOverview } from '@konfigyr/components/artifactory/registry/artifact-overview';

import type { ReactNode } from 'react';

function withAccount(children: ReactNode) {
  return (
    <AccountContext.Provider value={{ account: accounts.johnDoe, memberships: [namespaces.konfigyr, namespaces.johnDoe] }}>
      {children}
    </AccountContext.Provider>
  );
}

describe('components | registry | <ArtifactOverview/>', () => {
  afterEach(() => cleanup());

  test('should render artifact metadata', () => {
    const { getByRole, getByText } = renderWithQueryClient(withAccount(
      <ArtifactOverview namespace={namespaces.konfigyr} artifact={artifacts.publicArtifact}/>,
    ));

    expect(getByRole('heading', { name: 'com.example:public-service' })).toBeInTheDocument();
    expect(getByText('A publicly readable artifact.')).toBeInTheDocument();
    expect(getByRole('link', { name: artifacts.publicArtifact.website })).toBeInTheDocument();
  });

  test('should show the change visibility action for an admin of the owning namespace', async () => {
    const { findByRole } = renderWithQueryClient(withAccount(
      <ArtifactOverview namespace={namespaces.konfigyr} artifact={artifacts.publicArtifact}/>,
    ));

    expect(await findByRole('button', { name: 'Make private' })).toBeInTheDocument();
  });

  test('should not show the change visibility action for a non-admin member', async () => {
    const queryClient = createTestQueryClient();

    const { queryByRole } = renderWithQueryClient(withAccount(
      <ArtifactOverview namespace={namespaces.johnDoe} artifact={artifacts.publicArtifact}/>,
    ), { queryClient });

    await waitFor(() => {
      expect(queryClient.getQueryData(membershipKeys.getNamespaceMembers(namespaces.johnDoe.slug))).toBeDefined();
    });

    expect(queryByRole('button', { name: 'Make private' })).not.toBeInTheDocument();
  });
});
