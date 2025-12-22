import { afterEach, describe, expect, test } from 'vitest';
import { NamespaceRole } from '@konfigyr/hooks/namespace/types';
import { Invitations } from '@konfigyr/components/namespace/members/invitations';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { accounts } from '@konfigyr/test/helpers/mocks';
import { cleanup, within } from '@testing-library/react';

import type { Invitation } from '@konfigyr/hooks/types';

const invitation: Invitation = {
  key: 'invitation-key',
  sender: {
    id: accounts.johnDoe.id,
    email: accounts.johnDoe.email,
    name: accounts.johnDoe.fullName,
  },
  recipient: {
    email: accounts.janeDoe.email,
    name: accounts.johnDoe.fullName,
  },
  role: NamespaceRole.USER,
  expired: false,
  createdAt: '12/21/2025',
  expiryDate: '12/28/2025',
};

describe('components | namespace | members | <Invitations/>', () => {

  afterEach(() => cleanup());

  test('should render empty namespace invitations', () => {
    const result = renderWithQueryClient(<Invitations />);

    expect(result.getByText('No invitations found')).toBeInTheDocument();
  });

  test('should render pending namespace invitations', () => {
    const result = renderWithQueryClient(
      <Invitations invitations={[invitation]} />,
    );

    const table = result.getByRole('table');
    expect(table).toBeInTheDocument();

    const headers = within(table).getAllByRole('columnheader');
    expect(headers).toHaveLength(5);
    expect(headers[0]).toHaveTextContent('Recipient');
    expect(headers[1]).toHaveTextContent('Role');
    expect(headers[2]).toHaveTextContent('Sender');
    expect(headers[3]).toHaveTextContent('Send date');
    expect(headers[4]).toHaveTextContent('Expiration date');

    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(2);

    const cells = within(rows[1]).getAllByRole('cell');
    expect(cells).toHaveLength(5);
    expect(cells[0]).toHaveTextContent(invitation.recipient.name + invitation.recipient.email);
    expect(cells[1]).toHaveTextContent('User');
    expect(cells[2]).toHaveTextContent(invitation.sender.name + invitation.sender.email);
    expect(cells[3]).toHaveTextContent('12/21/2025');
    expect(cells[4]).toHaveTextContent('12/28/2025');
  });
});
