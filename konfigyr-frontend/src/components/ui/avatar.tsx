'use client';

import { cva } from 'class-variance-authority';
import { Avatar as AvatarPrimitive } from '@base-ui/react/avatar';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';
import type { VariantProps } from 'class-variance-authority';

export const avatarVariants = cva(
  'relative flex shrink-0 overflow-hidden border rounded-full',
  {
    variants: {
      size: {
        default: 'size-8',
        sm: 'size-6',
        lg: 'size-12',
        xl: 'size-16',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  },
);

export function Avatar({ size, className, ...props }: ComponentProps<typeof AvatarPrimitive.Root> & VariantProps<typeof avatarVariants>) {
  return (
    <AvatarPrimitive.Root
      data-slot="avatar"
      data-size={size}
      className={cn(avatarVariants({ size, className }))}
      {...props}
    />
  );
}

export function AvatarImage({ className, ...props }: ComponentProps<typeof AvatarPrimitive.Image>) {
  return (
    <AvatarPrimitive.Image
      data-slot="avatar-image"
      className={cn('"aspect-square size-full rounded-full object-cover', className)}
      {...props}
    />
  );
}

export function AvatarFallback({ className, ...props }: ComponentProps<typeof AvatarPrimitive.Fallback>) {
  return (
    <AvatarPrimitive.Fallback
      data-slot="avatar-fallback"
      className={cn(
        'flex size-full items-center justify-center rounded-full bg-muted text-sm text-muted-foreground group-data-[size=sm]/avatar:text-xs',
        className,
      )}
      {...props}
    />
  );
}

export function AvatarBadge({ className, ...props }: ComponentProps<'span'>) {
  return (
    <span
      data-slot="avatar-badge"
      className={cn(
        'absolute right-0 bottom-0 z-10 inline-flex items-center justify-center rounded-full bg-primary text-primary-foreground bg-blend-color ring-2 ring-background select-none',
        'group-data-[size=sm]/avatar:size-2 group-data-[size=sm]/avatar:[&>svg]:hidden',
        'group-data-[size=default]/avatar:size-2.5 group-data-[size=default]/avatar:[&>svg]:size-2',
        'group-data-[size=lg]/avatar:size-3 group-data-[size=lg]/avatar:[&>svg]:size-2',
        'group-data-[size=xl]/avatar:size-3.5 group-data-[size=xl]/avatar:[&>svg]:size-2',
        className,
      )}
      {...props}
    />
  );
}

export function AvatarGroup({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="avatar-group"
      className={cn('group/avatar-group flex -space-x-2 *:data-[slot=avatar]:ring-2 *:data-[slot=avatar]:ring-background', className)}
      {...props}
    />
  );
}

export function AvatarGroupCount({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="avatar-group-count"
      className={cn(
        'relative flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-sm text-muted-foreground ring-2 ring-background [&>svg]:size-4',
        'group-has-data-[size=sm]/avatar-group:size-6 group-has-data-[size=sm]/avatar-group:[&>svg]:size-3',
        'group-has-data-[size=lg]/avatar-group:size-10 group-has-data-[size=lg]/avatar-group:[&>svg]:size-5',
        'group-has-data-[size=xl]/avatar-group:size-12 group-has-data-[size=lg]/avatar-group:[&>svg]:size-6',
        className,
      )}
      {...props}
    />
  );
}
