import { ActivityIcon, TrendingDownIcon } from 'lucide-react';
import { CardIcon } from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

export function ActivityCardTitle({ className, children, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="activity-card-title"
      className={cn('flex items-center gap-2 px-6 leading-none text-lg font-medium', className)}
      {...props}
    >
      <CardIcon>
        <ActivityIcon size="1.25rem"/>
      </CardIcon>

      {children}
    </div>
  );
}

export function ActivityCardContent({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="activity-card-content"
      className={cn('px-6', className)}
      {...props}
    />
  );
}

export function ActivityCardEmpty({ ...props }: ComponentProps<typeof EmptyState>) {
  return (
    <EmptyState
      icon={<TrendingDownIcon />}
      {...props}
    />
  );
}

export function ActivityCard({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="activity-card"
      className={cn('bg-report-card text-stats-report-foreground flex flex-col gap-4 rounded-xl shadow py-4', className)}
      {...props}
    />
  );
}
