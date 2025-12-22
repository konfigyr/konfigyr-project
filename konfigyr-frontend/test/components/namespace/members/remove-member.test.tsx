import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { NamespaceRole } from '@konfigyr/hooks/namespace/types';
import { RemoveMemberForm } from '@konfigyr/components/namespace/members/remove-member';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { Member } from '@konfigyr/hooks/namespace/types';

const render = (member: Member | null, onClose: () => void) => renderWithQueryClient((
  <>
    <RemoveMemberForm namespace={namespaces.konfigyr} member={member} onClose={onClose} />
    <Toaster />
  </>
));

describe('components | namespace | members | <RemoveMemberForm/>', () => {
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

  test('should not render remove namespace member alert dialog when member is not selected', async () => {
    const result = render(null, onClose);

    await waitFor(() => {
      expect(result.queryByRole('alertdialog')).toBeNull();
    });
  });

  test('should render remove namespace member alert dialog', async () => {
    const result = render(member, onClose);

    await waitFor(() => {
      expect(result.getByRole('alertdialog')).toBeInTheDocument();
      expect(result.getByRole('alertdialog')).toHaveAccessibleName('Removing namespace member');
      expect(result.getByRole('alertdialog')).toHaveAccessibleDescription(
        'Are your sure you want to remove Namespace Member from your namespace?',
      );
      expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
      expect(result.getByRole('button', { name: 'Yes, remove' })).toBeInTheDocument();
    });
  });

  test('should invoke on close action when cancel button is clicked', async () => {
    const result = render(member, onClose);

    await userEvents.click(
      result.getByRole('button', { name: 'Cancel' }),
    );

    expect(onClose).toHaveBeenCalledOnce();
  });

  test('should fail to remove namespace member', async () => {
    const result = render(member, onClose);

    await userEvents.click(
      result.getByRole('button', { name: 'Yes, remove' }),
    );

    await waitFor(() => {
      expect(result.getByText('Not found')).toBeInTheDocument();
    });

    expect(onClose).not.toHaveBeenCalled();
  });

  test('should remove namespace member and close alert dialog', async () => {
    const result = render({
      ...member,
      id: 'remove-member',
    }, onClose);

    await userEvents.click(
      result.getByRole('button', { name: 'Yes, remove' }),
    );

    await waitFor(() => {
      expect(result.getByText(`Successfully removed ${member.fullName} from ${namespaces.konfigyr.name}`))
        .toBeInTheDocument();
    });

    expect(onClose).toHaveBeenCalledOnce();
  });
});
