import { afterAll, beforeAll, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

import type { RenderResult } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';

describe('routes | namespace | provision', () => {
  let result: RenderResult & { router: { state: { location: { pathname: string } } } };
  let user: UserEvent;

  beforeAll(() => {
    user = userEvents.setup();
    result = renderWithRouter('/namespace/provision');
  });

  afterAll(() => cleanup());

  test('should render Namespace provisioning card', async () => {
    await waitFor(() => {
      expect(result.getByText('Tell us more about your organization or team that would be the owner of this namespace.'))
        .toBeInTheDocument();

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
      result.getByRole('button'),
    );

    expect(result.getByRole('button'))
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

    // wait for the debounce period to be over to avoid race conditions when validating the URL slug
    await new Promise((resolve) => setTimeout(resolve, 300));

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'URL' })).toBeValid();
    });

    expect(result.getByRole('textbox', { name: 'URL' }))
      .toHaveValue('available-namespace');
  });

  test('should create namespace and redirect to namespace overview page', async () => {
    expect(result.getByRole('button')).not.toBeDisabled();

    await user.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.router.state.location.pathname).toBe('/namespace/available-namespace');
    });
  });
});
