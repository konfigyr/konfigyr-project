import { useMemo } from 'react';
import { MinusIcon, PlusIcon } from 'lucide-react';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { PropertyName } from '@konfigyr/components/artifactory/property-name';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { cn } from '@konfigyr/components/utils';
import { PropertyTransitionTypeLabel } from './property-transition-type';

import type { ComponentProps, ReactNode } from 'react';
import type { PropertyTransition } from '@konfigyr/hooks/vault/types';

export function useGroupedPropertyTransitions<T extends PropertyTransition>(transitions: Array<T> = []) {
  return useMemo(
    () => transitions.reduce((state, transition) => {
      const { action } = transition;
      if (!state[action]) {
        state[action] = [];
      }
      state[action].push(transition);
      return state;
    }, {} as Record<PropertyTransitionType, Array<T> | undefined>),
    [transitions],
  );
}

export function PropertyTransitionItemSkeleton() {
  return (
    <div data-slot="property-transition-item-skeleton" className="flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <Skeleton className="size-5 rounded-full" />
        <Skeleton className="h-4 w-16" />
      </div>
      <Skeleton className="h-3 w-48" />
      <Skeleton className="h-8" />
      <div className="flex items-center gap-2">
        <Skeleton className="size-5 rounded-full" />
        <Skeleton className="h-4 w-20" />
      </div>
      <Skeleton className="h-3 w-96" />
      <Skeleton className="h-16" />
    </div>
  );
}

export function PropertyTransitionValue({ variant, value, className, ...props }: {
  variant: 'addition' | 'removal';
  value: ReactNode;
} & ComponentProps<'div'>) {
  return (
    <div
      data-slot="property-transition-value"
      className={cn(
        'flex items-center gap-2 px-3 py-1 leading-loose',
        variant === 'removal' && 'bg-destructive/5 text-destructive/80',
        variant === 'addition' && 'bg-emerald-500/5 text-emerald-700',
        className,
      )}
      {...props}
    >
      <span className="select-none shrink-0">
        {variant === 'removal' ? <MinusIcon className="size-2" /> : <PlusIcon className="size-2" />}
      </span>
      <span className="break-all font-medium">{value}</span>
    </div>
  );
}

export function PropertyTransitionItem({ transition, className, ...props }: {
  transition: PropertyTransition;
} & ComponentProps<'div'>) {
  return (
    <div
      data-slot="property-transition-item"
      className={cn('flex flex-col text-xs', className)}
      {...props}
    >
      <PropertyName
        value={transition.name}
        className="text-xs text-foreground/90 leading-loose overflow-hidden text-ellipsis whitespace-nowrap"
        title={transition.name}
      />

      {transition.action === PropertyTransitionType.ADDED && (
        <PropertyTransitionValue
          variant="addition"
          value={transition.to}
        />
      )}

      {transition.action === PropertyTransitionType.UPDATED && (
        <span className="overflow-hidden border rounded-md">
          <PropertyTransitionValue
            className="border-b border-border/50"
            variant="removal"
            value={transition.from}
          />
          <PropertyTransitionValue
            variant="addition"
            value={transition.to}
          />
        </span>
      )}

      {transition.action === PropertyTransitionType.REMOVED && (
        <PropertyTransitionValue
          variant="removal"
          value={transition.from}
        />
      )}
    </div>
  );
}

export function PropertyTransitionItemGroup({ type, transitions = [], ...props }: {
  type: PropertyTransitionType,
  transitions?: Array<PropertyTransition>,
} & ComponentProps<'div'>) {
  if (transitions.length === 0) {
    return null;
  }

  return (
    <div data-slot="property-transition-item-group" {...props}>
      <PropertyTransitionTypeLabel type={type} />

      {transitions.map(transition => (
        <PropertyTransitionItem key={transition.name} transition={transition} />
      ))}
    </div>
  );
}
