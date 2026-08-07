import { useCallback } from 'react';
import { Link2OffIcon } from 'lucide-react';
import { FormattedDate, FormattedMessage } from 'react-intl';
import { useAcceptInvitation, useDeclineInvitation } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { NamespaceRoleBadge } from '@konfigyr/components/namespace/role';
import { Button } from '@konfigyr/components/ui/button';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@konfigyr/components/ui/table';
import { toast } from '@konfigyr/components/ui/toast';

import type { Invitation } from '@konfigyr/hooks/types';

export function AccountInvitations({ invitations }: { invitations: Array<Invitation> }) {
  const errorNotification = useErrorNotification();

  return (
    <Table variant="card">
      <TableHeader>
        <TableRow>
          <TableHead>
            <FormattedMessage
              defaultMessage="Namespace"
              description="Label for the namespace column in the account invitations table."
            />
          </TableHead>
          <TableHead>
            <FormattedMessage
              defaultMessage="Role"
              description="Label for the role column in the account invitations table."
            />
          </TableHead>
          <TableHead>
            <FormattedMessage
              defaultMessage="Sender"
              description="Label for the sender column in the account invitations table."
            />
          </TableHead>
          <TableHead>
            <FormattedMessage
              defaultMessage="Created at"
              description="Label for the created at column in the account invitations table."
            />
          </TableHead>
          <TableHead>
            <FormattedMessage
              defaultMessage="Expires at"
              description="Label for the expires at column in the account invitations table."
            />
          </TableHead>
          <TableHead>
            <span className="sr-only">
              <FormattedMessage
                defaultMessage="Actions"
                description="Label for the actions column in the account invitations table."
              />
            </span>
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {invitations.length === 0 && (
          <TableRow>
            <TableCell colSpan={6}>
              <EmptyState
                title="No pending invitations"
                description="You don't have any pending invitations to accept or decline."
                icon={<Link2OffIcon size="2rem" />}
              />
            </TableCell>
          </TableRow>
        )}

        {invitations.map(invitation => (
          <AccountInvitationRow
            key={invitation.key}
            invitation={invitation}
            onError={errorNotification}
          />
        ))}
      </TableBody>
    </Table>
  );
}

function AccountInvitationRow({ invitation, onError }: { invitation: Invitation, onError: (error: unknown) => void }) {
  const { isPending: isAccepting, mutateAsync: acceptInvitation } = useAcceptInvitation(invitation.key);
  const { isPending: isDeclining, mutateAsync: declineInvitation } = useDeclineInvitation(invitation.key);

  const onAccept = useCallback(async () => {
    try {
      await acceptInvitation();
    } catch (error) {
      return onError(error);
    }

    return toast.add({
      type: 'success',
      title: (
        <FormattedMessage
          defaultMessage="Successfully joined {namespace}"
          values={{ namespace: invitation.organization.name }}
          description="Success message when an account invitation is accepted"
        />
      ),
    });
  }, [invitation, acceptInvitation, onError]);

  const onDecline = useCallback(async () => {
    try {
      await declineInvitation();
    } catch (error) {
      return onError(error);
    }

    return toast.add({
      type: 'success',
      title: (
        <FormattedMessage
          defaultMessage="Declined invitation to {namespace}"
          values={{ namespace: invitation.organization.name }}
          description="Success message when an account invitation is declined"
        />
      ),
    });
  }, [invitation, declineInvitation, onError]);

  return (
    <TableRow>
      <TableCell>
        <p className="font-medium">{invitation.organization.name}</p>
      </TableCell>
      <TableCell>
        <NamespaceRoleBadge role={invitation.role} variant="outline" />
      </TableCell>
      <TableCell>
        <p className="font-medium">{invitation.sender.name}</p>
        <p className="text-sm text-muted-foreground font-mono">{invitation.sender.email}</p>
      </TableCell>
      <TableCell>
        <FormattedDate value={invitation.createdAt} />
      </TableCell>
      <TableCell>
        <FormattedDate value={invitation.expiryDate} />
      </TableCell>
      <TableCell>
        <div className="flex items-center justify-end gap-2">
          <Button
            size="sm"
            variant="outline"
            disabled={isDeclining || isAccepting}
            loading={isDeclining}
            onClick={onDecline}
          >
            <FormattedMessage
              defaultMessage="Decline"
              description="Label for the decline button in the account invitations table."
            />
          </Button>
          <Button
            size="sm"
            disabled={isDeclining || isAccepting}
            loading={isAccepting}
            onClick={onAccept}
          >
            <FormattedMessage
              defaultMessage="Accept"
              description="Label for the accept button in the account invitations table."
            />
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}
