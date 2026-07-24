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
      <div className="font-medium text-xs text-report-card-foreground/70">
        {title}
      </div>
      <div className="font-heading font-semibold text-3xl text-report-card-accent-foreground py-1">
        {counter}
      </div>
      {cta && (
        <div className="text-xs text-primary [&_svg]:size-4">
          {cta}
        </div>
      )}
      {footer && (
        <div className="text-xs text-report-card-foreground/70">
          {footer}
        </div>
      )}
    </div>
  );
}

export function StatsCard({ className, children, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="stats-card"
      className={cn('bg-report-card text-report-card-foreground flex flex-col gap-2 rounded-xl shadow py-4', className)}
      {...props}
    >
      <div
        data-slot="stats-card-content"
        className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 px-6"
      >
        {children}
      </div>
    </div>
  );
}
