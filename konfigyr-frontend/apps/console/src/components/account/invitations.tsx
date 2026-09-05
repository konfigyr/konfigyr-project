import { useCallback, useState } from 'react';
import { Link2OffIcon } from 'lucide-react';
import { FormattedDate, FormattedMessage } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { useAcceptInvitation, useAccountContext, useDeclineInvitation, useLastUsedNamespace } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { NamespaceRoleBadge } from '@konfigyr/components/namespace/role';
import { Button, buttonVariants } from '@konfigyr/components/ui/button';
import { Card, CardContent } from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/components/ui/select';
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

  if (invitations.length === 0) {
    return (
      <div className="lg:w-1/3 xl:w1/5 mx-auto p-4">
        <AccountInvitationsEmptyState />
      </div>
    );
  }

  return (
    <div className="container">
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
          {invitations.map(invitation => (
            <AccountInvitationRow
              key={invitation.key}
              invitation={invitation}
              onError={errorNotification}
            />
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function AccountInvitationsEmptyState() {
  const { memberships } = useAccountContext();
  const [lastUsedNamespace] = useLastUsedNamespace();
  const [namespace, setNamespace] = useState<string | undefined>(lastUsedNamespace ?? memberships[0]?.slug);

  const selected = memberships.find(membership => membership.slug === namespace);

  return (
    <Card>
      <CardContent>
        <EmptyState
          title="No pending invitations"
          description="You don't have any pending invitations to accept or decline."
          icon={<Link2OffIcon size="2rem" />}
        >
          <div className="flex w-full flex-col gap-3">
            {memberships.length > 0 && (
              <div className="flex items-center gap-2">
                <Select value={namespace} onValueChange={value => setNamespace(value ?? undefined)}>
                  <SelectTrigger className="w-full">
                    <SelectValue>{selected?.name}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {memberships.map(membership => (
                      <SelectItem key={membership.slug} value={membership.slug}>
                        {membership.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Link
                  to="/namespace/$namespace"
                  params={{ namespace: namespace ?? memberships[0].slug }}
                  className={buttonVariants()}
                >
                  <FormattedMessage
                    defaultMessage="Go"
                    description="Label for the button that takes the user to the selected namespace once every account invitation has been resolved."
                  />
                </Link>
              </div>
            )}
            <Link
              to="/namespace/provision"
              className={buttonVariants({ variant: memberships.length > 0 ? 'ghost' : 'default' })}
            >
              <FormattedMessage
                defaultMessage="Create a namespace"
                description="Label for the button that takes the user to namespace provisioning once every account invitation has been resolved."
              />
            </Link>
          </div>
        </EmptyState>
      </CardContent>
    </Card>
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
