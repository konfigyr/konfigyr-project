import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, fireEvent } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ChangeRequestMergeStatus } from '@konfigyr/hooks/vault/types';
import { ChangeRequestMergeStateCard } from '@konfigyr/components/vault/change-request/change-request-merge-state';

describe('components | vault | change-request | <ChangeRequestMergeStateCard/>', () => {
  const onMerge = vi.fn() as () => void | Promise<any>;

  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  test('should render <ChangeRequestMergeStateCard/> component for not approved state', () => {
    const { container, getByRole, getByText } = renderWithMessageProvider(
      <ChangeRequestMergeStateCard value={ChangeRequestMergeStatus.NOT_APPROVED} onMerge={onMerge} />,
    );

    expect(getByText('Review required')).toBeInTheDocument();
    expect(getByText('At least one review is required before the change request can be merged.')).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-circle-x');

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button')).toBeDisabled();
  });

  test('should render <ChangeRequestMergeStateCard/> component for not open state', () => {
    const { container, getByRole, getByText } = renderWithMessageProvider(
      <ChangeRequestMergeStateCard value={ChangeRequestMergeStatus.NOT_OPEN} onMerge={onMerge} />,
    );

    expect(getByText('Change request not open')).toBeInTheDocument();
    expect(getByText('Change request was either merged or discarded.')).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-pull-request-closed');

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button')).toBeDisabled();
  });

  test('should render <ChangeRequestMergeStateCard/> component for changes requested state', () => {
    const { container, getByRole, getByText } = renderWithMessageProvider(
      <ChangeRequestMergeStateCard value={ChangeRequestMergeStatus.CHANGES_REQUESTED} onMerge={onMerge} />,
    );

    expect(getByText('Additional changes required')).toBeInTheDocument();
    expect(getByText('At least one reviewer requested additional changes before the change request can be merged.'))
      .toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-message-circle-plus');

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button')).toBeDisabled();
  });

  test('should render <ChangeRequestMergeStateCard/> component for checking state', () => {
    const { container, getByRole, getByText } = renderWithMessageProvider(
      <ChangeRequestMergeStateCard value={ChangeRequestMergeStatus.CHECKING} onMerge={onMerge} />,
    );

    expect(getByText('Checking...')).toBeInTheDocument();
    expect(getByText('We are checking the mergeability of this change request. This may take a few minutes.')).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-compare-arrows');

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button')).toBeDisabled();
  });

  test('should render <ChangeRequestMergeStateCard/> component for conflicting state', () => {
    const { container, getByRole, getByText } = renderWithMessageProvider(
      <ChangeRequestMergeStateCard value={ChangeRequestMergeStatus.CONFLICTING} onMerge={onMerge} />,
    );

    expect(getByText('Conflicts detected')).toBeInTheDocument();
    expect(getByText('The change request cannot be merged because of merge conflicts. Please clone this change request, resolve the conflicts and submit your changes again.'))
      .toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-merge-conflict');

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button')).toBeDisabled();
  });

  test('should render <ChangeRequestMergeStateCard/> component for outdated state', () => {
    const { container, getByRole, getByText } = renderWithMessageProvider(
      <ChangeRequestMergeStateCard value={ChangeRequestMergeStatus.OUTDATED} onMerge={onMerge} />,
    );

    expect(getByText('Change request outdated')).toBeInTheDocument();
    expect(getByText('The change request is outdated. Please rebase the changes from the target profile.')).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-merge-conflict');

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button')).toBeDisabled();
  });

  test('should render <ChangeRequestMergeStateCard/> component for mergeable state', () => {
    const { container, getByRole, getByText } = renderWithMessageProvider(
      <ChangeRequestMergeStateCard value={ChangeRequestMergeStatus.MERGEABLE} onMerge={onMerge} />,
    );

    expect(getByText('Ready to merge')).toBeInTheDocument();
    expect(getByText('Your change request is ready to be merged. Click the merge button to proceed.')).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-circle-check');

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button')).not.toBeDisabled();

    fireEvent.click(getByRole('button'));

    expect(onMerge).toHaveBeenCalledExactlyOnceWith();
  });

});
