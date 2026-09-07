import { useMemo } from 'react';
import { cn } from '@konfigyr/ui/lib/utils';
import { Badge } from '@konfigyr/ui/components/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/ui/components/tooltip';

import type { ComponentProps } from 'react';

function PropertyTypeNameBadge({ value, className, ...props }: { value: string } & ComponentProps<typeof Badge>) {
  return (
    <Badge variant="outline" size="sm" className={cn('font-normal', className)} {...props}>
      {value}
    </Badge>
  );
}

export function PropertyTypeName({ value, className, ...props }: { value: string } & ComponentProps<typeof Badge>) {
  const { truncated, text } = useMemo(() => {
    if (value.length > 52) {
      return { truncated: true, text: value.substring(0, 52) + '...' };
    }
    return { truncated: false, text: value };
  }, [value]);

  if (truncated) {
    return (
      <Tooltip>
        <TooltipTrigger delay={200} render={<PropertyTypeNameBadge value={text} {...props} />} />
        <TooltipContent side="top" className="font-mono text-xs max-w-full">
          {value}
        </TooltipContent>
      </Tooltip>
    );
  }

  return (
    <PropertyTypeNameBadge value={text} {...props} />
  );
}
