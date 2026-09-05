import { cva } from 'class-variance-authority';
import { cn } from '@konfigyr/components/utils';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';

import type { VariantProps } from 'class-variance-authority';
import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';

export const typeVariants = cva(
  'font-mono text-xs',
  {
    variants: {
      type: {
        default: 'bg-muted! text-muted-foreground! border-border',
        string: 'text-emerald-600! dark:text-emerald-400! border-emerald-600',
        number: 'text-blue-600! dark:text-blue-400! border-blue-600',
        integer: 'text-blue-600! dark:text-blue-400! border-blue-600',
        boolean: 'text-amber-600! dark:text-amber-400! border-amber-600',
        array: 'text-purple-600! dark:text-purple-400! border-purple-600',
        object: 'text-fuchsia-600! dark:text-fuchsia-400! border-fuchsia-600',
      },
      variant: {
        default: '',
        badge: 'border rounded-4xl px-1.5 py-0.2 text-[0.65rem] h-4.25',
      },
    },
  },
);

export type TypeVariants = VariantProps<typeof typeVariants>;

export function PropertySchema({ value, variant, className }: {
  value?: PropertyJsonSchema,
  variant?: TypeVariants['variant'],
  className?: string
}) {
  const type = value?.type as TypeVariants['type'];

  return (
    <Tooltip>
      <TooltipTrigger delay={400} className={cn(typeVariants({ type, variant }), className)}>
        {type}
      </TooltipTrigger>
      <TooltipContent>
        <div className="max-h-80 overflow-y-auto">
          <pre>{JSON.stringify(value, null, 2)}</pre>
        </div>
      </TooltipContent>
    </Tooltip>
  );
}
