import { Slot } from 'radix-ui';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

export function PropertyName({
  value,
  className,
  asChild = false,
  ...props
}: { value: string, asChild?: boolean } & ComponentProps<'span'>) {
  const Component = asChild ? Slot.Root : 'span';

  return (
    <Component
      data-slot="vault-property-name"
      className={cn('font-mono text-sm font-medium text-foreground', className)}
      {...props}
    >
      {value}
    </Component>
  );
}
