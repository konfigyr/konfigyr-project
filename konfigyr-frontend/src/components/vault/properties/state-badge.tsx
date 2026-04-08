import { useIntl } from 'react-intl';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';
import { labelForTransitionType } from '@konfigyr/components/vault/messages';
import { Badge } from '@konfigyr/components/ui/badge';
import { cn } from '@konfigyr/components/utils';

import type { BadgeProps } from '@konfigyr/components/ui/badge';

export type StateBadgeProps = { variant: ConfigurationPropertyState } & Omit<BadgeProps, 'variant'>;

export function StateBadge({ className, variant, ...props }: StateBadgeProps) {
  const intl = useIntl();

  switch (variant) {
    case ConfigurationPropertyState.ADDED:
      return (
        <Badge
          size="sm"
          variant="outline"
          className={cn('border-emerald-400/40 text-emerald-600 dark:text-emerald-400', className)}
          {...props}
        >
          {labelForTransitionType(intl, variant)}
        </Badge>
      );
    case ConfigurationPropertyState.UPDATED:
      return (
        <Badge
          size="sm"
          variant="outline"
          className={cn('border-amber-400/40 text-amber-600 dark:text-amber-400', className)}
          {...props}
        >
          {labelForTransitionType(intl, variant)}
        </Badge>
      );
    case ConfigurationPropertyState.REMOVED:
      return (
        <Badge
          size="sm"
          variant="outline"
          className={cn('border-destructive/40 text-destructive', className)}
          {...props}
        >
          {labelForTransitionType(intl, variant)}
        </Badge>
      );
    default:
      return null;
  }
}
