import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { ChangesetStatusBar } from '@konfigyr/components/vault/changeset/status-bar';
import { useChangesetState } from '@konfigyr/hooks';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';

import type { Profile } from '@konfigyr/hooks/types';

function TestChangesetStatusBar({ profile }: { profile: Profile }) {
  const { data: changeset } = useChangesetState(namespaces.konfigyr, services.konfigyrApi, profile);

  return changeset && (
    <>
      <Toaster />
      <ChangesetStatusBar changeset={changeset} />
    </>
  );
}

describe('components | vault | changeset | <ChangesetStatusBa/>', () => {
  afterEach(() => cleanup());

  test('should render changeset status bar for unprotected profile', async () => {
    const { getByRole } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.development} />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Apply' })).toBeInTheDocument();
      expect(getByRole('button', { name: 'Discard' })).toBeInTheDocument();
      expect(getByRole('button', { name: 'Changeset draft' })).toBeInTheDocument();
    });
  });

  test('should render changeset status bar for protected profile', async () => {
    const { getByRole } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.staging} />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Submit' })).toBeInTheDocument();
      expect(getByRole('button', { name: 'Discard' })).toBeInTheDocument();
      expect(getByRole('button', { name: 'Changeset draft' })).toBeInTheDocument();
    });
  });

  test('should render changeset status bar for immutable profile', async () => {
    const { getByRole, queryByRole } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.deprecated} />,
    );

    await waitFor(() => {
      expect(queryByRole('button', { name: 'Apply' })).not.toBeInTheDocument();
      expect(queryByRole('button', { name: 'Submit' })).not.toBeInTheDocument();
      expect(getByRole('button', { name: 'Discard' })).toBeInTheDocument();
      expect(getByRole('button', { name: 'Changeset draft' })).toBeInTheDocument();
    });
  });

  test('should update changeset name', async () => {
    const { getByRole } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.development} />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Changeset draft' })).toBeInTheDocument();
    });

    await userEvent.click(
      getByRole('button', { name: 'Changeset draft' }),
    );

    await userEvent.clear(
      getByRole('textbox'),
    );

    await userEvent.type(
      getByRole('textbox'),
      'Updated changeset name',
    );

    await userEvent.click(
      getByRole('button', { name: 'Save' }),
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Updated changeset name' })).toBeInTheDocument();
    });
  });

  test('should discard changeset', async () => {
    const { getByRole, getByText } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.development} />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Discard' })).toBeInTheDocument();
    });

    await userEvent.click(
      getByRole('button', { name: 'Discard' }),
    );

    await waitFor(() => {
      expect(getByText('Your changes were discarded')).toBeInTheDocument();
    });
  });

  test('should apply changeset', async () => {
    const { getByRole, getByText } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.development} />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Apply' })).toBeInTheDocument();
    });

    await userEvent.click(
      getByRole('button', { name: 'Apply' }),
    );

    await waitFor(() => {
      expect(getByText('Your changes were successfully applied')).toBeInTheDocument();
    });
  });
});
