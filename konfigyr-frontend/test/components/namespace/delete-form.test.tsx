import { afterAll, afterEach, beforeAll, describe, expect, test, vi } from 'vitest';
import { NamespaceDeleteForm } from '@konfigyr/components/namespace/form/delete';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | namespace | <NamespaceDeleteForm/>', () => {
  let result: RenderResult;
  const onDelete = vi.fn();

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <NamespaceDeleteForm namespace={namespaces.konfigyr} onDelete={onDelete} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render namespace delete confirmation card', () => {
    expect(result.getByRole('button')).toBeInTheDocument();
    expect(result.getByRole('button')).toHaveAccessibleName('Delete namespace');
    expect(result.getByRole('paragraph')).toHaveTextContent(
      'Permanently remove this namespace and all its associated services, configurations, and metadata. This action is irreversible',
    );

    expect(result.queryAllByRole('alertdialog')).to.have.lengthOf(0);
  });

  test('should open namespace delete confirmation dialog', async () => {
    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByRole('alertdialog')).toBeInTheDocument();
      expect(result.getByRole('alertdialog')).toHaveAccessibleName('Are you sure?');
      expect(result.getByRole('alertdialog')).toHaveAccessibleDescription(
        'This action cannot be undone. This will permanently delete the namespace and all its associated data from our servers.',
      );
    });
  });

  test('should close namespace delete confirmation dialog', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Cancel' }),
    );

    await waitFor(() => {
      expect(result.queryAllByRole('alertdialog')).to.have.lengthOf(0);
    });
  });

  test('should fail to delete namespace due to unexpected error', async () => {
    onDelete.mockImplementationOnce(() => {
      return Promise.reject(new Error('Fail to delete account'));
    });

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByRole('button', { name: 'Delete' })).toBeInTheDocument();
    });

    await userEvents.click(
      result.getByRole('button', { name: 'Delete' }),
    );

    await waitFor(() => {
      expect(result.getByText('Unexpected server error occurred')).toBeInTheDocument();
    });
  });

  test('should successfully delete namespace', async () => {
    expect(result.getByRole('alertdialog')).toBeInTheDocument();

    await userEvents.click(
      result.getByRole('button', { name: 'Delete' }),
    );

    await waitFor(() => {
      expect(onDelete).toHaveBeenCalledExactlyOnceWith(namespaces.konfigyr);
    });
  });
});
