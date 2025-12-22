import { afterAll, beforeAll, describe, expect, test } from 'vitest';
import { NamespaceNameForm } from '@konfigyr/components/namespace/form/name';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | namespace | <NamespaceNameForm/>', () => {
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <NamespaceNameForm namespace={namespaces.konfigyr} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());

  test('should render namespace name form', async () => {
    await waitFor(() => {
      expect(result.getByRole('textbox')).toBeInTheDocument();
      expect(result.getByRole('textbox')).toHaveAccessibleName('Name');
      expect(result.getByRole('textbox')).toHaveAccessibleDescription(
        'The official name of your namespace. This is how your team, project, or organization will be identified. Keep it clear and recognizable!',
      );
    });

    await waitFor(() => {
      expect(result.getByRole('button', { name: 'Update display name' })).toBeInTheDocument();
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

  test('should enter invalid namespace name', async () => {
    await userEvents.type(
      result.getByRole('textbox'),
      'Namespace name that is too long and should trigger validation error',
    );

    expect(result.getByRole('textbox')).toBeInvalid();
    expect(result.getByRole('button')).toBeDisabled();
  });

  test('should submit form with invalid display name', async () => {
    await userEvents.clear(
      result.getByRole('textbox'),
    );

    await userEvents.type(
      result.getByRole('textbox'),
      'Invalid name',
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

  test('should submit form with valid display name', async () => {
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
      expect(result.getByText('Your namespace name was updated')).toBeInTheDocument();
    });

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });
});
