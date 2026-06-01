import { useIntl } from 'react-intl';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';
import { labelForTransitionType } from '@konfigyr/components/vault/messages';
import { Badge } from '@konfigyr/components/ui/badge';

import type { BadgeProps } from '@konfigyr/components/ui/badge';

export type StateBadgeProps = { variant: ConfigurationPropertyState } & Omit<BadgeProps, 'variant'>;

export function StateBadge({ variant, ...props }: StateBadgeProps) {
  const intl = useIntl();

  switch (variant) {
    case ConfigurationPropertyState.ADDED:
      return (
        <Badge
          size="sm"
          variant="success"
          {...props}
        >
          {labelForTransitionType(intl, variant)}
        </Badge>
      );
    case ConfigurationPropertyState.UPDATED:
      return (
        <Badge
          size="sm"
          variant="warning"
          {...props}
        >
          {labelForTransitionType(intl, variant)}
        </Badge>
      );
    case ConfigurationPropertyState.REMOVED:
      return (
        <Badge
          size="sm"
          variant="destructive"
          {...props}
        >
          {labelForTransitionType(intl, variant)}
        </Badge>
      );
    default:
      return null;
  }
}
