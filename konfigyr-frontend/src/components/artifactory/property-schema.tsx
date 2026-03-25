import { cva } from 'class-variance-authority';
import { cn } from '@konfigyr/components/utils';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';

import type { ComponentProps } from 'react';
import type { VariantProps } from 'class-variance-authority';
import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';

export const typeVariants = cva(
  'font-mono text-xs',
  {
    variants: {
      type: {
        default: 'bg-muted text-muted-foreground',
        string: 'text-emerald-600 dark:text-emerald-400',
        number: 'text-blue-600 dark:text-blue-400',
        integer: 'text-blue-600 dark:text-blue-400',
        boolean: 'text-amber-600 dark:text-amber-400',
        array: 'text-purple-600 dark:text-purple-400',
        object: 'text-fuchsia-600 dark:text-fuchsia-400',
      },
    },
  },
);

export type TypeVariants = VariantProps<typeof typeVariants>;

export function PropertySchema({ value, className, ...props }: { value?: PropertyJsonSchema } & ComponentProps<'span'>) {
  const type = value?.type as TypeVariants['type'];

  return (
    <Tooltip delayDuration={400}>
      <TooltipTrigger asChild>
        <span className={cn(className, typeVariants({ type }))} {...props}>
          {type}
        </span>
      </TooltipTrigger>
      <TooltipContent className="max-h-60 overflow-y-auto">
        <pre>
          <code>{JSON.stringify(value, null, 2)}</code>
        </pre>
      </TooltipContent>
    </Tooltip>
  );
}
