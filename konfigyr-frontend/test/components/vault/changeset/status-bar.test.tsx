import { useMemo } from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { ConfigurationPropertyState, PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { ChangesetStatusBar } from '@konfigyr/components/vault/changeset/status-bar';
import { useChangesetState } from '@konfigyr/hooks';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';

import type { Profile } from '@konfigyr/hooks/types';
import type { ChangesetStatusBarProps } from '@konfigyr/components/vault/changeset/status-bar';

function TestChangesetStatusBar({ profile, modified = false, ...props }: {
  profile: Profile;
  modified?: boolean;
} & Omit<ChangesetStatusBarProps, 'changeset'>) {
  const { data } = useChangesetState(namespaces.konfigyr, services.konfigyrApi, profile);

  const changeset = useMemo(() => {
    if (data && modified) {
      data.properties.push({
        name: 'spring.application.name',
        typeName: 'java.lang.String',
        schema: { type: 'string' },
        value: { encoded: 'test-app', decoded: 'test-app' },
        state: PropertyTransitionType.ADDED,
      });
      data.added = data.properties.filter(p => p.state === ConfigurationPropertyState.ADDED).length;
      data.modified = data.properties.filter(p => p.state === ConfigurationPropertyState.UPDATED).length;
      data.deleted = data.properties.filter(p => p.state === ConfigurationPropertyState.REMOVED).length;
    }
    return data;
  }, [data, modified]);

  return changeset && (
    <>
      <Toaster />
      <ChangesetStatusBar changeset={changeset} {...props} />
    </>
  );
}

describe('components | vault | changeset | <ChangesetStatusBar/>', () => {
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
    const onRenamed = vi.fn();
    const { getByRole } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.development} onChangesetRenamed={onRenamed}/>,
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

    expect(onRenamed).toHaveBeenCalledOnce();
  });

  test('should discard changeset', async () => {
    const onDiscarded = vi.fn();

    const { getByRole, getByText } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.development} onChangesetDiscarded={onDiscarded} />,
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

    expect(onDiscarded).toHaveBeenCalledOnce();
  });

  test('should disable the submit changeset dialog trigger button when no changes are present', async () => {
    const { getByRole } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.development} />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Apply' })).toBeInTheDocument();
    });

    expect(getByRole('button', { name: 'Apply' })).toBeDisabled();
  });

  test('should open the submit changeset dialog and apply the changes to an unprotected profile', async () => {
    const onApplied = vi.fn();

    const { getByRole, getByText } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.development} modified={true} onChangesetApplied={onApplied} />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Apply' })).toBeInTheDocument();
      expect(getByRole('button', { name: 'Apply' })).not.toBeDisabled();
    });

    await userEvent.click(
      getByRole('button', { name: 'Apply' }),
    );

    await waitFor(() => {
      expect(getByRole('dialog')).toBeInTheDocument();
    });

    await userEvent.click(
      getByRole('button', { name: 'Propose changes' }),
    );

    await waitFor(() => {
      expect(getByText('Your changes were successfully applied')).toBeInTheDocument();
    });

    expect(onApplied).toHaveBeenCalledOnce();
  });

  test('should open the submit changeset dialog and submit the changes to a protected profile', async () => {
    const onSubmitted = vi.fn();

    const { getByRole, getByText } = renderWithQueryClient(
      <TestChangesetStatusBar profile={profiles.staging} modified={true} onChangeRequestCreated={onSubmitted}/>,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Submit' })).toBeInTheDocument();
    });

    await userEvent.click(
      getByRole('button', { name: 'Submit' }),
    );

    await waitFor(() => {
      expect(getByRole('dialog')).toBeInTheDocument();
    });

    await userEvent.click(
      getByRole('button', { name: 'Propose changes' }),
    );

    await waitFor(() => {
      expect(getByText('Change request created')).toBeInTheDocument();
      expect(getByText('Your changes were submitted for review and are now waiting for approval.')).toBeInTheDocument();
    });

    expect(onSubmitted).toHaveBeenCalledOnce();
  });
});
