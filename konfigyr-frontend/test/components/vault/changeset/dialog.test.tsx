import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { ChangesetSubmitDialog } from '@konfigyr/components/vault/changeset/dialog';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';

import type { ChangesetState } from '@konfigyr/hooks/types';

const changeset: ChangesetState = {
  namespace: namespaces.konfigyr,
  service: services.konfigyrApi,
  profile: profiles.development,
  added: 0,
  deleted: 0,
  modified: 0,
  name: 'Testing changeset',
  properties: [],
  state: 'DRAFT',
};

describe('components | vault | changeset | <ChangesetSubmitDialog/>', () => {
  afterEach(() => cleanup());

  test('should render a closed changeset submit dialog for unprotected profile', () => {
    const { getByRole, queryByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={false}
        changeset={changeset}
        onApply={vi.fn()}
        onSubmit={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(getByRole('button', { name: 'Apply' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Apply' })).not.toBeDisabled();
    expect(queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('should render a closed changeset submit dialog for protected profile', () => {
    const { getByRole, queryByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={false}
        changeset={{ ...changeset, profile: profiles.staging }}
        onApply={vi.fn()}
        onSubmit={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(getByRole('button', { name: 'Submit' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Submit' })).not.toBeDisabled();
    expect(queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('should not render any dialog or dialog trigger when profile is immutable', () => {
    const { queryByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={true}
        changeset={{ ...changeset, profile: profiles.deprecated }}
        onApply={vi.fn()}
        onSubmit={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(queryByRole('button')).not.toBeInTheDocument();
    expect(queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('should render an open changeset submit dialog', () => {
    const { queryByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={true}
        changeset={changeset}
        onApply={vi.fn()}
        onSubmit={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(queryByRole('dialog')).toBeInTheDocument();

    expect(queryByRole('textbox', { name: 'Change subject' })).toBeInTheDocument();
    expect(queryByRole('textbox', { name: 'Change subject' })).toHaveValue(changeset.name);

    expect(queryByRole('textbox', { name: 'Description' })).toBeInTheDocument();
    expect(queryByRole('textbox', { name: 'Description' })).toHaveValue('');

    expect(queryByRole('radiogroup')).toBeInTheDocument();
    expect(queryByRole('radio', { name: 'Apply changes directly' })).toBeInTheDocument();
    expect(queryByRole('radio', { name: 'Submit for review' })).toBeInTheDocument();

    expect(queryByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(queryByRole('button', { name: 'Propose changes' })).toBeInTheDocument();
  });

  test('should preselect apply changes directly option when using an unprotected profile', () => {
    const { queryByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={true}
        changeset={changeset}
        onApply={vi.fn()}
        onSubmit={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(queryByRole('radio', { name: 'Apply changes directly' })).toBeChecked();
    expect(queryByRole('radio', { name: 'Apply changes directly' })).not.toBeDisabled();

    expect(queryByRole('radio', { name: 'Submit for review' })).not.toBeChecked();
    expect(queryByRole('radio', { name: 'Submit for review' })).not.toBeDisabled();
  });

  test('should preselect submit for review option when using a protected profile', () => {
    const { queryByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={true}
        changeset={{ ...changeset, profile: profiles.staging }}
        onApply={vi.fn()}
        onSubmit={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(queryByRole('radio', { name: 'Apply changes directly' })).not.toBeChecked();
    expect(queryByRole('radio', { name: 'Apply changes directly' })).toHaveAttribute('aria-disabled', 'true');

    expect(queryByRole('radio', { name: 'Submit for review' })).toBeChecked();
    expect(queryByRole('radio', { name: 'Submit for review' })).not.toHaveAttribute('aria-disabled');
  });

  test('should close the changeset submit dialog when clicking on cancel button', async () => {
    const onClose = vi.fn();
    const { getByRole, queryByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={true}
        changeset={changeset}
        onOpenChange={onClose}
        onApply={vi.fn()}
        onSubmit={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(queryByRole('dialog')).toBeInTheDocument();
    expect(queryByRole('button', { name: 'Close' })).toBeInTheDocument();

    await userEvent.click(
      getByRole('button', { name: 'Close' }),
    );

    expect(onClose).toHaveBeenCalledOnce();
  });

  test('should submit changes without adding a description', async () => {
    const onApply = vi.fn();
    const onSubmit = vi.fn();

    const { getByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={true}
        changeset={changeset}
        onApply={onApply}
        onSubmit={onSubmit}
        onError={vi.fn()}
      />,
    );

    await userEvent.clear(
      getByRole('textbox', { name: 'Change subject' }),
    );

    await userEvent.type(
      getByRole('textbox', { name: 'Change subject' }),
      'New changeset name',
    );

    await userEvent.click(
      getByRole('radio', { name: 'Submit for review' }),
    );

    await userEvent.click(
      getByRole('button', { name: 'Propose changes' }),
    );

    expect(onApply).not.toHaveBeenCalled();
    expect(onSubmit).toHaveBeenCalledExactlyOnceWith({
      ...changeset, name: 'New changeset name', description: '',
    });
  });

  test('should apply changes with description', async () => {
    const onApply = vi.fn();
    const onSubmit = vi.fn();

    const { getByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={true}
        changeset={changeset}
        onApply={onApply}
        onSubmit={onSubmit}
        onError={vi.fn()}
      />,
    );

    await userEvent.clear(
      getByRole('textbox', { name: 'Change subject' }),
    );

    await userEvent.type(
      getByRole('textbox', { name: 'Change subject' }),
      'New changeset name',
    );

    await userEvent.type(
      getByRole('textbox', { name: 'Description' }),
      'Description of the changeset',
    );

    await userEvent.click(
      getByRole('button', { name: 'Propose changes' }),
    );

    expect(onSubmit).not.toHaveBeenCalled();
    expect(onApply).toHaveBeenCalledExactlyOnceWith({
      ...changeset,
      name: 'New changeset name',
      description: 'Description of the changeset',
    });
  });

  test('should submit changes to unprotected profile', async () => {
    const onApply = vi.fn();
    const onSubmit = vi.fn();

    const { getByRole } = renderWithQueryClient(
      <ChangesetSubmitDialog
        open={true}
        changeset={{ ...changeset, profile: profiles.staging }}
        onApply={onApply}
        onSubmit={onSubmit}
        onError={vi.fn()}
      />,
    );

    await userEvent.click(
      getByRole('radio', { name: 'Submit for review' }),
    );

    await userEvent.click(
      getByRole('button', { name: 'Propose changes' }),
    );

    expect(onApply).not.toHaveBeenCalled();
    expect(onSubmit).toHaveBeenCalledExactlyOnceWith({
      ...changeset,
      description: '',
      profile: profiles.staging,
    });
  });
});
