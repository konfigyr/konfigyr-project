import { afterEach, describe, expect, test } from 'vitest';
import { Invitations } from '@konfigyr/components/namespace/members/invitations';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor, within } from '@testing-library/react';

describe('components | namespace | members | <Invitations/>', () => {

  afterEach(() => cleanup());

  test('should render empty namespace invitations', async () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <Invitations namespace={namespaces.johnDoe} />,
    );

    expect(getByRole('table')).toBeInTheDocument();

    await waitFor(() => {
      expect(getByText('No invitations found')).toBeInTheDocument();
    });
  });

  test('should render namespace invitations in a loading state', () => {
    const { container, getByRole } = renderComponentWithRouter(
      <Invitations namespace={namespaces.konfigyr} />,
    );

    expect(getByRole('table')).toBeInTheDocument();

    expect(container.querySelector('[data-slot="invitations-skeleton"]')).toBeInTheDocument();
  });

  test('should render pending namespace invitations', async () => {
    const { getAllByRole, getByRole } = renderComponentWithRouter(
      <Invitations namespace={namespaces.konfigyr} />,
    );

    const table = getByRole('table');
    expect(table).toBeInTheDocument();

    const headers = within(table).getAllByRole('columnheader');
    expect(headers).toHaveLength(5);
    expect(headers[0]).toHaveTextContent('Recipient');
    expect(headers[1]).toHaveTextContent('Role');
    expect(headers[2]).toHaveTextContent('Sender');
    expect(headers[3]).toHaveTextContent('Created at');
    expect(headers[4]).toHaveTextContent('Expires at');

    await waitFor(() => {
      expect(getAllByRole('row')).toHaveLength(4);
    });

    const rows = getAllByRole('row');
    const cells = within(rows[1]).getAllByRole('cell');
    expect(cells).toHaveLength(5);
    expect(cells[0]).toHaveTextContent('recipient-user@konfigyr.com');
    expect(cells[1]).toHaveTextContent('User');
    expect(cells[2]).toHaveTextContent('John Doejohn.doe@konfigyr.com');
    expect(cells[3]).toHaveTextContent('4/17/2026');
    expect(cells[4]).toHaveTextContent('4/28/2026');
  });
});
