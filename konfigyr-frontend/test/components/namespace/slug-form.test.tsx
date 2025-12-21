import { afterAll, beforeAll, describe, expect, test } from 'vitest';
import { NamespaceSlugForm } from '@konfigyr/components/namespace/form/slug';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | namespace | <NamespaceSlugForm/>', () => {
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <NamespaceSlugForm namespace={namespaces.konfigyr} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());

  test('should render namespace slug form', async () => {
    await waitFor(() => {
      expect(result.getByRole('textbox')).toBeInTheDocument();
      expect(result.getByRole('textbox')).toHaveAccessibleName('Rename namespace');
      expect(result.getByRole('textbox')).toHaveAccessibleDescription(
        'The unique URL-friendly identifier for this namespace. Auto-generated from the name, but you can tweak it if needed. No spaces, just dashes!',
      );
    });

    await waitFor(() => {
      expect(result.getByRole('button', { name: 'Rename' })).toBeInTheDocument();
    });
  });

  test('should fail to submit invalid form', async () => {
    await userEvents.clear(
      result.getByRole('textbox'),
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    expect(result.getByRole('textbox')).toBeInvalid();
    expect(result.getByRole('button')).toBeDisabled();
  });

  test('should enter invalid namespace slug', async () => {
    await userEvents.type(
      result.getByRole('textbox'),
      'Namespace slug that is too long and should trigger validation error',
    );

    expect(result.getByRole('textbox')).toBeInvalid();
    expect(result.getByRole('button')).toBeDisabled();
  });

  test('should enter namespace slug that is already taken', async () => {
    await userEvents.type(
      result.getByRole('textbox'),
      'taken-slug',
    );

    expect(result.getByRole('textbox')).toBeInvalid();
    expect(result.getByRole('button')).toBeDisabled();
  });

  test('should submit form with valid URL slug', async () => {
    await userEvents.clear(
      result.getByRole('textbox'),
    );

    await userEvents.type(
      result.getByRole('textbox'),
      namespaces.konfigyr.slug,
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Your namespace was successfully renamed')).toBeInTheDocument();
    });

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });
});
