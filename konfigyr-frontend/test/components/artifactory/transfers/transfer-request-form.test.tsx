import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { TransferRequestForm } from '@konfigyr/components/artifactory/transfers/transfer-request-form';

const defaultValues = { groupId: '', fromNamespace: '' };

describe('components | transfers | <TransferRequestForm/>', () => {
  afterEach(() => cleanup());

  test('should show a hint before a groupId is chosen', () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <TransferRequestForm namespace={namespaces.konfigyr.slug} defaultValues={defaultValues} onSubmit={vi.fn()} onCancel={vi.fn()}/>,
    );

    expect(getByRole('combobox', { name: 'Group Id' })).toBeInTheDocument();
    expect(getByText('Select a groupId first.')).toBeInTheDocument();
  });

  test('should only list active group claims in the combobox', async () => {
    const user = userEvent.setup();
    const { getByRole } = renderComponentWithRouter(
      <TransferRequestForm namespace={namespaces.konfigyr.slug} defaultValues={defaultValues} onSubmit={vi.fn()} onCancel={vi.fn()}/>,
    );

    await user.click(getByRole('combobox', { name: 'Group Id' }));

    await waitFor(() => {
      expect(getByRole('option', { name: 'com.example.group' })).toBeInTheDocument();
      expect(getByRole('option', { name: 'com.acme.transfer-ready' })).toBeInTheDocument();
    });
  });

  test('should populate from-namespace radio options once a groupId is selected', async () => {
    const user = userEvent.setup();
    const { getByRole } = renderComponentWithRouter(
      <TransferRequestForm namespace={namespaces.konfigyr.slug} defaultValues={defaultValues} onSubmit={vi.fn()} onCancel={vi.fn()}/>,
    );

    await user.click(getByRole('combobox', { name: 'Group Id' }));
    await user.click(await waitFor(() => getByRole('option', { name: 'com.acme.transfer-ready' })));

    await waitFor(() => {
      expect(getByRole('radio', { name: 'ebf' })).toBeInTheDocument();
    });
  });

  test('should show an empty state when the selected groupId has no known owners', async () => {
    const user = userEvent.setup();
    const { getByRole, getByText } = renderComponentWithRouter(
      <TransferRequestForm namespace={namespaces.konfigyr.slug} defaultValues={defaultValues} onSubmit={vi.fn()} onCancel={vi.fn()}/>,
    );

    await user.click(getByRole('combobox', { name: 'Group Id' }));
    await user.click(await waitFor(() => getByRole('option', { name: 'com.example.group' })));

    await waitFor(() => {
      expect(getByText('No known namespaces own artifacts under this groupId yet.')).toBeInTheDocument();
    });
  });

  test('should reset the from-namespace choice when the groupId changes', async () => {
    const user = userEvent.setup();
    const { getByRole, getByText, queryByRole } = renderComponentWithRouter(
      <TransferRequestForm namespace={namespaces.konfigyr.slug} defaultValues={defaultValues} onSubmit={vi.fn()} onCancel={vi.fn()}/>,
    );

    await user.click(getByRole('combobox', { name: 'Group Id' }));
    await user.click(await waitFor(() => getByRole('option', { name: 'com.acme.transfer-ready' })));
    await user.click(await waitFor(() => getByRole('radio', { name: 'ebf' })));

    await user.click(getByRole('combobox', { name: 'Group Id' }));
    await user.click(await waitFor(() => getByRole('option', { name: 'com.example.group' })));

    await waitFor(() => {
      expect(getByRole('combobox', { name: 'Group Id' })).toHaveValue('com.example.group');
      expect(getByText('No known namespaces own artifacts under this groupId yet.')).toBeInTheDocument();
    });

    expect(queryByRole('radio', { name: 'ebf' })).not.toBeInTheDocument();
  });

  test('should show a link to claim a groupId when there are no active claims', async () => {
    const user = userEvent.setup();
    const { getByRole } = renderComponentWithRouter(
      <TransferRequestForm namespace={namespaces.johnDoe.slug} defaultValues={defaultValues} onSubmit={vi.fn()} onCancel={vi.fn()}/>,
    );

    await user.click(getByRole('combobox', { name: 'Group Id' }));

    await waitFor(() => {
      expect(getByRole('link', { name: 'Claim a groupId' })).toBeInTheDocument();
    });

    expect(getByRole('link', { name: 'Claim a groupId' })).toHaveAttribute('href', '/namespace/john-doe/artifactory/groups/create');
  });

  test('should submit the selected groupId and fromNamespace', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    const { getByRole } = renderComponentWithRouter(
      <TransferRequestForm namespace={namespaces.konfigyr.slug} defaultValues={defaultValues} onSubmit={onSubmit} onCancel={vi.fn()}/>,
    );

    await user.click(getByRole('combobox', { name: 'Group Id' }));
    await user.click(await waitFor(() => getByRole('option', { name: 'com.acme.transfer-ready' })));
    await user.click(await waitFor(() => getByRole('radio', { name: 'ebf' })));

    await user.click(getByRole('button', { name: 'Request transfer' }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
        value: { groupId: 'com.acme.transfer-ready', fromNamespace: 'ebf' },
      }));
    });
  });

  test('should render the passed-in error inline', () => {
    const { getByText } = renderComponentWithRouter(
      <TransferRequestForm
        namespace={namespaces.konfigyr.slug}
        defaultValues={defaultValues}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
        error={{ title: 'Bad request', detail: 'GroupId \'unverified.group\' is not verified for publishing' }}
      />,
    );

    expect(getByText('GroupId \'unverified.group\' is not verified for publishing')).toBeInTheDocument();
  });

  test('should call onCancel when the cancel button is clicked', async () => {
    const onCancel = vi.fn();
    const user = userEvent.setup();
    const { getByRole } = renderComponentWithRouter(
      <TransferRequestForm namespace={namespaces.konfigyr.slug} defaultValues={defaultValues} onSubmit={vi.fn()} onCancel={onCancel}/>,
    );

    await user.click(getByRole('button', { name: 'Cancel' }));

    expect(onCancel).toHaveBeenCalledOnce();
  });
});
