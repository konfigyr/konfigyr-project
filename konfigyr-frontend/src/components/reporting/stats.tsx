import { Card, CardContent } from '@konfigyr/components/ui/card';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps, ReactNode } from 'react';

export function CounterStat({ title, counter, cta, footer, className }: {
  title: ReactNode;
  counter: ReactNode;
  cta?: ReactNode;
  footer?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn('px-2 border-r last:border-none', className)}>
      <div className="font-heading font-medium text-sm uppercase leading-relaxed text-muted-foreground">
        {title}
      </div>
      <div className="font-heading font-semibold text-4xl py-2">
        {counter}
      </div>
      {cta && (
        <div className="text-xs text-primary [&_svg]:size-4">
          {cta}
        </div>
      )}
      {footer && (
        <div className="text-xs text-muted-foreground">
          {footer}
        </div>
      )}
    </div>
  );
}

export function StatsCard({ className, children, size = 1, ...props }: ComponentProps<'div'> & { size?: number }) {
  return (
    <Card className={cn('border', className)} {...props}>
      <CardContent className={cn('grid grid-cols-1 gap-4 px-6', `grid-cols-${size}`)}>
        {children}
      </CardContent>
    </Card>
  );
}
