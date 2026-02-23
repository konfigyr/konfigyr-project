import { FormattedMessage } from 'react-intl';
import { cva } from 'class-variance-authority';
import { Badge } from '@konfigyr/components/ui/badge';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';
import type { VariantProps } from 'class-variance-authority';
import type { BadgeProps } from '@konfigyr/components/ui/badge';

export const stateLabelVariants = cva(
  'flex items-center gap-1 text-xs',
  {
    variants: {
      variant: {
        added: '[&>*:first-child]:bg-emerald-600',
        deprecated: '[&>*:first-child]:bg-destructive',
        deleted: '[&>*:first-child]:bg-destructive',
        modified: '[&>*:first-child]:bg-amber-600',
      },
    },
  },
);

export const stateBadgeVariants = cva(
  'text-xs px-1.5 py-0 h-4 font-normal shrink-0',
  {
    variants: {
      variant: {
        added: 'border-emerald-400/40 text-emerald-600 dark:text-emerald-400',
        deprecated: 'border-destructive/40 text-destructive',
        deleted: 'border-destructive/40 text-destructive',
        modified: 'border-amber-400/40 text-amber-600 dark:text-amber-400',
      },
    },
  },
);

const labelForVariant = (variant?: string | null) => {
  switch (variant) {
    case 'added':
      return <FormattedMessage defaultMessage="added" description="Label for added state badge" />;
    case 'deprecated':
      return <FormattedMessage defaultMessage="deprecated" description="Label for deprecated state badge" />;
    case 'deleted':
      return <FormattedMessage defaultMessage="deleted" description="Label for deleted state badge" />;
    case 'modified':
      return <FormattedMessage defaultMessage="modified" description="Label for modified state badge" />;
    default:
      throw new Error(`Unknown state badge variant: ${variant}`);
  }
};

export function StateLabel({
  className,
  variant,
  count,
  ...props
}: ComponentProps<'p'> & VariantProps<typeof stateLabelVariants> & { count?: number }) {
  return (
    <p className={cn(className, stateLabelVariants({ variant }))} {...props}>
      <span className="size-2 rounded-sm shrink-0" />
      {count && (
        <span className="text-foreground font-medium tabular-nums">{count}</span>
      )}
      <span className="text-muted-foreground hidden sm:inline">
        {labelForVariant(variant)}
      </span>
    </p>
  );
}

export function StateBadge({
  className,
  variant,
  ...props
}: Omit<BadgeProps, 'variant'> & VariantProps<typeof stateBadgeVariants>) {
  return (
    <Badge className={cn(className, stateBadgeVariants({ variant }))} variant="outline" {...props}>
      {labelForVariant(variant)}
    </Badge>
  );
}
