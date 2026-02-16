import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import {userEvent} from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { ChangesetEditor } from '@konfigyr/components/vault/changeset/editor';
import { profiles } from '@konfigyr/test/helpers/mocks';

import type { ChangesetState } from '@konfigyr/hooks/types';

const changeset: ChangesetState = {
  profile: profiles.development,
  name: 'test changeset',
  state: 'DRAFT',
  properties: [{
    name: 'application.name',
    description: 'Application name property',
    type: 'java.lang.String',
    state: 'modified',
    value: 'konfigyr-frontend',
    schema: {
      type: 'string',
    },
  }, {
    name: 'application.profile',
    description: 'Application profile property',
    type: 'java.lang.String',
    state: 'unchanged',
    value: 'staging',
    deprecation: {
      reason: 'This property is deprecated',
    },
    schema: {
      type: 'string',
      enum: ['staging', 'production'],
    },
  }],
  added: 0,
  modified: 1,
  deleted: 0,
};

describe('components | vault | changeset | <ChangesetEditor/>', () => {
  afterEach(() => cleanup());

  test('should render changeset editor without active filters', () => {
    const result = renderWithQueryClient(
      <ChangesetEditor
        changeset={changeset}
      />,
    );

    expect(result.getByRole('table')).toBeInTheDocument();
    expect(result.getAllByRole('row')).length(3);
  });

  test('should filter changeset properties by term', async () => {
    const result = renderWithQueryClient(
      <ChangesetEditor
        changeset={changeset}
      />,
    );

    expect(result.getByRole('textbox')).toBeInTheDocument();

    await userEvent.type(result.getByRole('textbox'), 'name');

    await waitFor(() => {
      expect(result.getAllByRole('row')).length(2);
    });
  });

  test('should filter changeset properties by state', async () => {
    const result = renderWithQueryClient(
      <ChangesetEditor
        changeset={changeset}
      />,
    );

    expect(result.getByRole('textbox')).toBeInTheDocument();

    await userEvent.click(result.getByRole('radio', { name: 'Modified' }));

    await waitFor(() => {
      expect(result.getByText('application.name')).toBeInTheDocument();
      expect(result.getAllByRole('row')).length(2);
    });

    await userEvent.click(result.getByRole('radio', { name: 'Deleted' }));

    await waitFor(() => {
      expect(result.queryByText('application.name')).toBeNull();
      expect(result.getAllByRole('row')).length(2);

      expect(result.queryByText('No configuration properties found.')).toBeInTheDocument();
    });
  });

  test('should display an empty state when no filter matches the property', async () => {
    const result = renderWithQueryClient(
      <ChangesetEditor
        changeset={changeset}
      />,
    );

    await userEvent.click(result.getByRole('radio', { name: 'Deleted' }));

    await waitFor(() => {
      expect(result.getAllByRole('row')).length(2);
      expect(result.queryByText('No configuration properties found.')).toBeInTheDocument();
    });
  });

  test('should open changeset property history', async () => {
    const result = renderWithQueryClient(
      <ChangesetEditor
        changeset={changeset}
      />,
    );

    const buttons = result.getAllByRole('button', { name: 'History' });
    expect(buttons).length(2);

    await userEvent.click(buttons[0]);

    await waitFor(() => {
      expect(result.queryByRole('dialog')).toBeInTheDocument();
    });
  });

  test('should add changeset property value', async () => {
    const result = renderWithQueryClient(
      <ChangesetEditor
        changeset={changeset}
      />,
    );

    const button = result.getByRole('button', { name: 'Add property' });
    expect(button).toBeInTheDocument();

    await userEvent.click(button);

    await waitFor(() => {
      expect(result.getByRole('dialog', { name: 'Add configuration property' })).toBeInTheDocument();
      expect(result.getByRole('textbox', { name: 'Property name' })).toBeInTheDocument();
      expect(result.getByRole('textbox', { name: 'Value' })).toBeInTheDocument();
    });

    await userEvent.type(
      result.getByRole('textbox', { name: 'Property name' }),
      'application.version',
    );

    await userEvent.type(
      result.getByRole('textbox', { name: 'Value' }),
      '1.0.0',
    );

    await userEvent.click(result.getByRole('button', { name: 'Add property' }));

    await waitFor(() => {
      expect(result.queryByRole('dialog', { name: 'Add configuration property' })).toBeNull();
    });
  });

  test('should update changeset property value', async () => {
    const result = renderWithQueryClient(
      <ChangesetEditor
        changeset={changeset}
      />,
    );

    const button = result.getByRole('button', { name: 'konfigyr-frontend' });
    expect(button).toBeInTheDocument();

    await userEvent.click(button);

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'application.name' })).toBeInTheDocument();
    });
  });

  test('should delete changeset property history', async () => {
    const result = renderWithQueryClient(
      <ChangesetEditor
        changeset={changeset}
      />,
    );

    const buttons = result.getAllByRole('button', { name: 'Delete' });
    expect(buttons).length(2);

    await userEvent.click(buttons[0]);

    await waitFor(() => {
      expect(result.queryByText('Deleted')).toBeInTheDocument();
    });
  });
});
