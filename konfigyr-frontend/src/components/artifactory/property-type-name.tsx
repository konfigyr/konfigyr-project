import { useMemo } from 'react';
import { cn } from '@konfigyr/components/utils';
import { Badge } from '@konfigyr/components/ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';

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
      <Tooltip delayDuration={200}>
        <TooltipTrigger asChild>
          <PropertyTypeNameBadge value={text} {...props} />
        </TooltipTrigger>
        <TooltipContent side="top" className="font-mono text-xs">
          {value}
        </TooltipContent>
      </Tooltip>
    );
  }

  return (
    <PropertyTypeNameBadge value={text} {...props} />
  );
}
