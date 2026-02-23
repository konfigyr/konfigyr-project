import { Slot } from 'radix-ui';
import { cn } from '@konfigyr/components/utils';
import { MissingPropertyDescriptionLabel } from './messages';

import type { ComponentProps } from 'react';

export function PropertyDescription({
  value,
  className,
  asChild = false,
  ...props
}: { value?: string, asChild?: boolean } & ComponentProps<'span'>) {
  const Component = asChild ? Slot.Root : 'span';

  return (
    <Component
      data-slot="vault-property-descrption"
      className={cn('text-xs text-muted-foreground leading-relaxed wrap-break-word whitespace-pre-wrap', className)}
      {...props}
    >
      {value || <MissingPropertyDescriptionLabel />}
    </Component>
  );
}
