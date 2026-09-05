import { cn } from '@konfigyr/components/utils';
import { MissingPropertyDescriptionLabel } from './messages';

import type { ComponentProps } from 'react';

export function PropertyDescription({ value, className, ...props }: { value?: string } & ComponentProps<'span'>) {
  return (
    <span
      data-slot="artifactory-property-descrption"
      className={cn('text-xs text-muted-foreground leading-relaxed wrap-break-word whitespace-pre-wrap', className)}
      {...props}
    >
      {value || <MissingPropertyDescriptionLabel />}
    </span>
  );
}
