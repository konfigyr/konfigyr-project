import { PencilIcon, PlusIcon, TrashIcon } from 'lucide-react';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { useLabelForTransitionType } from '@konfigyr/components/vault/messages';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

const iconForTransitionType = (type: PropertyTransitionType, className?: string) => {
  switch (type) {
    case PropertyTransitionType.ADDED:
      return (
        <i className={cn('bg-emerald-500/20 text-emerald-500', className)}>
          <PlusIcon />
        </i>
      );
    case PropertyTransitionType.UPDATED:
      return (
        <i className={cn('bg-amber-500/20 text-amber-500', className)}>
          <PencilIcon />
        </i>
      );
    case PropertyTransitionType.REMOVED:
      return (
        <i className={cn('bg-destructive/20 text-destructive', className)}>
          <TrashIcon />
        </i>
      );
    default:
      throw new Error(`Unknown transition type: ${type}`);
  }
};

export function PropertyTransitionTypeLabel({ type, variant = 'default', className, ...props }: {
  type: PropertyTransitionType,
  variant?: 'default' | 'badge' | 'icon',
} & ComponentProps<'span'>) {
  const label = useLabelForTransitionType(type);

  return (
    <span
      data-slot="vault-property-transition-type"
      className={cn(variant !== 'icon' && 'inline-flex items-center gap-2', className)}
      aria-label={variant === 'icon' ? label : undefined}
      {...props}
    >
      {iconForTransitionType(type, 'size-5 rounded-full flex items-center justify-center *:size-2.5')}
      {variant !== 'icon' && (
        <span className="font-heading">
          {label}
        </span>
      )}
    </span>
  );
}
