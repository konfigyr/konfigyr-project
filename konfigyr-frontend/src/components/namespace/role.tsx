import { FormattedMessage } from 'react-intl';
import { NamespaceRole } from '@konfigyr/hooks/namespace/types';
import { Badge } from '@konfigyr/components/ui/badge';
import type { BadgeProps } from '@konfigyr/components/ui/badge';

export function NamespaceRoleLabel({ role }: { role: NamespaceRole }) {
  switch (role) {
    case NamespaceRole.USER: {
      return <FormattedMessage
        defaultMessage="User"
        description="Label for user namespace role"
      />;
    }
    case NamespaceRole.ADMIN: {
      return <FormattedMessage
        defaultMessage="Administrator"
        description="Label for admin namespace role"
      />;
    }
    default: {
      return null;
    }
  }
}

export function NamespaceRoleDescription({ role }: { role: NamespaceRole }) {
  switch (role) {
    case NamespaceRole.USER: {
      return <FormattedMessage
        defaultMessage="Can see every member and can create new projects, manage configuration and secrets."
        description="Description for user namespace role"
      />;
    }
    case NamespaceRole.ADMIN: {
      return <FormattedMessage
        defaultMessage="Has full administrative access to the entire namespace."
        description="Description for admin namespace role"
      />;
    }
    default: {
      throw new Error(`Unknown namespace role: ${role}`);
    }
  }
}

export function NamespaceRoleBadge({ role, ...props }: { role: NamespaceRole } & BadgeProps) {
  return (
    <Badge {...props}>
      <NamespaceRoleLabel role={role} />
    </Badge>
  );
}
