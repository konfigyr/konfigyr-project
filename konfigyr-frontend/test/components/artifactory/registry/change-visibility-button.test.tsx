import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor, within } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { artifacts, namespaces } from '@konfigyr/test/helpers/mocks';
import { ChangeVisibilityButton } from '@konfigyr/components/artifactory/registry/change-visibility-button';

const toast = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }));

vi.mock('sonner', () => ({ toast }));

describe('components | registry | <ChangeVisibilityButton/>', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should offer to make a PUBLIC artifact private', async () => {
    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient(
      <ChangeVisibilityButton namespace={namespaces.konfigyr.slug} artifact={artifacts.publicArtifact}/>,
    );

    await user.click(getByRole('button', { name: 'Make private' }));

    const dialog = await waitFor(() => getByRole('alertdialog'));
    expect(dialog).toHaveAccessibleName('Change artifact visibility');
  });

  test('should submit the visibility change successfully', async () => {
    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient(
      <ChangeVisibilityButton namespace={namespaces.konfigyr.slug} artifact={artifacts.publicArtifact}/>,
    );

    await user.click(getByRole('button', { name: 'Make private' }));

    const dialog = await waitFor(() => getByRole('alertdialog'));
    await user.click(within(dialog).getByRole('button', { name: 'Make private' }));

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledOnce();
    });
    expect(toast.error).not.toHaveBeenCalled();
  });

  test('should report an error when the artifact can not be found', async () => {
    const user = userEvents.setup();

    const unknownArtifact = { ...artifacts.publicArtifact, groupId: 'unknown.group' };

    const { getByRole } = renderWithQueryClient(
      <ChangeVisibilityButton namespace={namespaces.konfigyr.slug} artifact={unknownArtifact}/>,
    );

    await user.click(getByRole('button', { name: 'Make private' }));

    const dialog = await waitFor(() => getByRole('alertdialog'));
    await user.click(within(dialog).getByRole('button', { name: 'Make private' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledOnce();
    });
    expect(toast.success).not.toHaveBeenCalled();
  });
});
