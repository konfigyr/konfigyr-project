import { cn } from '@konfigyr/components/utils';

import type { ComponentProps, ReactNode } from 'react';

export function CounterStat({ title, counter, className }: { title: string | ReactNode, counter: number, className?: string }) {
  return (
    <div className={cn('border-r last:border-none', className)}>
      <div className="font-medium text-xs text-report-card-foreground/70">
        {title}
      </div>
      <div className="font-semibold text-lg text-report-card-accent-foreground py-1">
        {counter}
      </div>
    </div>
  );
}

export function StatsCard({ className, children, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="stats-card"
      className={cn('bg-report-card text-report-card-foreground flex flex-col gap-4 rounded-xl shadow py-4', className)}
      {...props}
    >
      <div
        data-slot="stats-card-content"
        className="grid grid-cols-2 md:grid-cols-4 gap-6 px-6"
      >
        {children}
      </div>
    </div>
  );
}
