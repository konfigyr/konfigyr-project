import { Loader2Icon } from 'lucide-react';

import { cn } from '@konfigyr/components/utils';
import type { ComponentProps } from 'react';

export function Spinner({ className, ...props }: ComponentProps<'svg'>) {
  return (
    <Loader2Icon
      data-slot="spinner"
      role="status"
      aria-label="Loading"
      className={cn('size-4 animate-spin', className)}
      {...props}
    />
  );
}
