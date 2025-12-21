import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { NamespaceRole } from '@konfigyr/hooks/namespace/types';
import { UpdateMemberForm } from '@konfigyr/components/namespace/members/update-member';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { Member } from '@konfigyr/hooks/namespace/types';

const render = (member: Member | null, onClose: () => void) => renderWithQueryClient((
  <>
    <UpdateMemberForm namespace={namespaces.konfigyr} member={member} onClose={onClose} />
    <Toaster />
  </>
));

describe('components | namespace | members | <UpdateMemberForm/>', () => {
  const member = {
    id: 'test-member',
    role: NamespaceRole.USER,
    email: 'member@konfigyr.com',
    fullName: 'Namespace Member',
  };

  let onClose: () => void;

  beforeEach(() => {
    onClose = vi.fn();
  });

  afterEach(() => cleanup());

  test('should not render update namespace member alert dialog when member is not selected', async () => {
    const result = render(null, onClose);

    await waitFor(() => {
      expect(result.queryByRole('dialog')).toBeNull();
    });
  });

  test('should render update namespace member alert dialog', async () => {
    const result = render(member, onClose);

    await waitFor(() => {
      expect(result.getByRole('dialog')).toBeInTheDocument();
      expect(result.getByRole('dialog')).toHaveAccessibleName('Change the role of Namespace Member?');
      expect(result.getByRole('dialog')).toHaveAccessibleDescription('Select a new role for this member.');
      expect(result.getByRole('radiogroup')).toBeInTheDocument();
      expect(result.getByRole('radio', { checked: false })).toBeInTheDocument();
      expect(result.getByRole('radio', { checked: false })).toHaveValue(NamespaceRole.ADMIN);
      expect(result.getByRole('radio', { checked: true })).toBeInTheDocument();
      expect(result.getByRole('radio', { checked: true })).toHaveValue(NamespaceRole.USER);
      expect(result.getByRole('button', { name: 'Close' })).toBeInTheDocument();
      expect(result.getByRole('button', { name: 'Update role' })).toBeInTheDocument();
    });
  });

  test('should invoke on close action when cancel button is clicked', async () => {
    const result = render(member, onClose);

    await userEvents.click(
      result.getByRole('button', { name: 'Close' }),
    );

    expect(onClose).toHaveBeenCalledOnce();
  });

  test('should fail to update namespace member', async () => {
    const result = render(member, onClose);

    await userEvents.click(
      result.getByRole('radio', { checked: false }),
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Update role' }),
    );

    await waitFor(() => {
      expect(result.getByText('Not found')).toBeInTheDocument();
    });

    expect(onClose).not.toHaveBeenCalled();
  });

  test('should update namespace member and close alert dialog', async () => {
    const result = render({
      ...member,
      id: 'update-member',
    }, onClose);

    await userEvents.click(
      result.getByRole('radio', { checked: false }),
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Update role' }),
    );

    await waitFor(() => {
      expect(result.getByText('Successfully updated member role')).toBeInTheDocument();
    });

    expect(onClose).toHaveBeenCalledOnce();
  });
});
