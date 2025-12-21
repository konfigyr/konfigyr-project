import { FormattedDate } from 'react-intl';
import { Link2OffIcon } from 'lucide-react';
import { NamespaceRoleBadge } from '@konfigyr/components/namespace/role';
import { Card, CardContent } from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@konfigyr/components/ui/table';
import type { Invitation } from '@konfigyr/hooks/types';

export function Invitations({ invitations }: { invitations?: Array<Invitation> }) {
  if (!invitations || invitations.length === 0) {
    return (
      <EmptyState
        title="No invitations found"
        description="There are currently no pending invitations for this namespace."
        icon={<Link2OffIcon size="2rem" />}
      />
    );
  }

  return (
    <Card>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Recipient</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Sender</TableHead>
              <TableHead>Send date</TableHead>
              <TableHead>Expiration date</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {invitations.map(invitation => (
              <TableRow key={invitation.key}>
                <TableCell>
                  {invitation.recipient.name && (
                    <p className="font-medium">{invitation.recipient.name}</p>
                  )}
                  <p className="text-sm text-muted-foreground font-mono">{invitation.recipient.email}</p>
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
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
