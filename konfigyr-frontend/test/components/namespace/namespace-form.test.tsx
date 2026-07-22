import { afterAll, beforeAll, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { CreateNamespaceForm } from '@konfigyr/components/namespace/form/create';

import type { RenderResult } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';

describe('components | namespace | <CreateNamespaceForm/>', () => {
  let result: RenderResult;
  let user: UserEvent;
  const onCreate = vi.fn();

  beforeAll(() => {
    user = userEvents.setup();
    result = renderWithQueryClient(
      <CreateNamespaceForm onCreate={onCreate} debounceMs={0} />,
    );
  });

  afterAll(() => cleanup());

  test('should render the namespace form fields', async () => {
    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Name' }))
        .toBeInTheDocument();

      expect(result.getByRole('textbox', { name: 'URL' }))
        .toBeInTheDocument();

      expect(result.getByRole('textbox', { name: 'Description' }))
        .toBeInTheDocument();
    });
  });

  test('should validate initial form data', async () => {
    await user.click(
      result.getByRole('button', { name: 'Create namespace' }),
    );

    expect(result.getByRole('button', { name: 'Create namespace' }))
      .toBeDisabled();

    expect(result.getByRole('textbox', { name: 'Name' }))
      .toBeInvalid();

    expect(result.getByRole('textbox', { name: 'URL' }))
      .toBeInvalid();
  });

  test('should connect name with URL slug input field', async () => {
    await user.type(
      result.getByRole('textbox', { name: 'Name' }),
      'some Namespace name',
    );

    expect(result.getByRole('textbox', { name: 'URL' })).toHaveValue('some-namespace-name');
  });

  test('should disconnect name when URL slug input field is made dirty and invalid', async () => {
    await user.clear(
      result.getByRole('textbox', { name: 'URL' }),
    );

    await user.type(
      result.getByRole('textbox', { name: 'URL' }),
      'updated-namespace-slug',
    );

    await user.clear(
      result.getByRole('textbox', { name: 'Name' }),
    );

    await user.type(
      result.getByRole('textbox', { name: 'Name' }),
      'Namespace name',
    );

    expect(result.getByRole('textbox', { name: 'URL' }))
      .toHaveValue('updated-namespace-slug');

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'URL' })).toBeInvalid();
    });
  });

  test('should validate URL slug availability', async () => {
    await user.clear(
      result.getByRole('textbox', { name: 'URL' }),
    );

    await user.type(
      result.getByRole('textbox', { name: 'URL' }),
      'available-namespace',
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'URL' })).toBeValid();
    });

    expect(result.getByRole('textbox', { name: 'URL' }))
      .toHaveValue('available-namespace');
  });

  test('should submit the form and invoke onCreate with the created namespace', async () => {
    await user.click(
      result.getByRole('button', { name: 'Create namespace' }),
    );

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledExactlyOnceWith(
        expect.objectContaining({ slug: 'available-namespace' }),
      );
    });
  });
});
