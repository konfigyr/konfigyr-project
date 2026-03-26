import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

export function PropertyName({ value, className, ...props }: { value: string } & ComponentProps<'span'>) {
  return (
    <span
      data-slot="artifactory-property-name"
      className={cn('font-mono text-sm font-medium text-foreground', className)}
      {...props}
    >
      {value}
    </span>
  );
}
