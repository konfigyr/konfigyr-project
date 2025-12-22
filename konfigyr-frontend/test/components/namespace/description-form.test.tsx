import { afterAll, beforeAll, describe, expect, test } from 'vitest';
import { NamespaceDescriptionForm } from '@konfigyr/components/namespace/form/description';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | namespace | <NamespaceDescriptionForm/>', () => {
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <NamespaceDescriptionForm namespace={namespaces.konfigyr} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());

  test('should render namespace description form', async () => {
    await waitFor(() => {
      expect(result.getByRole('textbox')).toBeInTheDocument();
      expect(result.getByRole('textbox')).toHaveAccessibleName('Description');
      expect(result.getByRole('textbox')).toHaveAccessibleDescription(
        'Tell the world (or just your team) what this namespace is all about. A short and sweet explanation of its purpose, goals, or mission. Or not...',
      );
    });

    await waitFor(() => {
      expect(result.getByRole('button', { name: 'Update description' })).toBeInTheDocument();
    });
  });

  test('should enter invalid namespace description', async () => {
    const array = new Uint8Array(192);
    crypto.getRandomValues(array);

    const length = array.byteLength;
    let description = '';

    for (let i = 0; i < length; i++) {
      description += String.fromCharCode(array[i]);
    }

    await userEvents.type(
      result.getByRole('textbox'),
      window.btoa(description),
    );

    expect(result.getByRole('textbox')).toBeInvalid();
    expect(result.getByRole('button')).toBeDisabled();
  });

  test('should submit form with invalid description', async () => {
    await userEvents.clear(
      result.getByRole('textbox'),
    );

    await userEvents.type(
      result.getByRole('textbox'),
      'Invalid description',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Bad request')).toBeInTheDocument();
    });

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });

  test('should submit form with valid description', async () => {
    await userEvents.clear(
      result.getByRole('textbox'),
    );

    await userEvents.type(
      result.getByRole('textbox'),
      namespaces.konfigyr.name,
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Your namespace description was updated')).toBeInTheDocument();
    });

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });
});
