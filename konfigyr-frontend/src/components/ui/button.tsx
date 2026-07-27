'use client';

import * as React from 'react';
import { Button as ButtonPrimitive } from '@base-ui/react/button';
import { cva } from 'class-variance-authority';
import { Spinner } from '@konfigyr/components/ui/spinner';
import { cn } from '@konfigyr/components/utils';

import type { VariantProps } from 'class-variance-authority';

export const buttonVariants = cva(
  'group/button inline-flex shrink-0 items-center justify-center rounded-full border-2 border-transparent bg-clip-padding text-sm font-bold whitespace-nowrap transition-all outline-none select-none active:not-aria-[haspopup]:translate-y-px disabled:pointer-events-none disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*="size-"])]:size-4',
  {
    variants: {
      variant: {
        default:
          'bg-primary text-primary-foreground hover:bg-(--btn-primary-hover-bg) active:bg-(--btn-primary-active-bg) focus-visible:bg-(--btn-primary-focus-bg) focus-visible:border-(--btn-primary-focus-border)',
        destructive:
          'bg-destructive/10 text-destructive hover:bg-destructive/20 focus-visible:border-destructive/40 focus-visible:ring-destructive/20 dark:bg-destructive/20 dark:hover:bg-destructive/30 dark:focus-visible:ring-destructive/40',
        outline:
          'border-primary bg-transparent text-primary hover:bg-primary hover:text-primary-foreground active:bg-(--btn-outline-active-bg) active:border-(--btn-outline-active-bg) active:text-primary-foreground aria-expanded:bg-(--btn-outline-active-bg) aria-expanded:border-(--btn-outline-active-bg) aria-expanded:text-primary-foreground focus-visible:bg-(--btn-outline-focus-bg) focus-visible:border-(--btn-outline-focus-border) focus-visible:text-primary-foreground',
        secondary:
          'bg-background text-primary hover:text-(--btn-secondary-hover-text) active:text-(--btn-secondary-active-text) aria-expanded:text-(--btn-secondary-active-text) focus-visible:text-(--btn-secondary-focus-text) focus-visible:border-(--btn-secondary-focus-border)',
        ghost:
          'text-primary hover:bg-(--btn-ghost-hover-bg) hover:text-(--btn-ghost-hover-text) active:bg-(--btn-ghost-active-bg) active:text-(--btn-ghost-hover-text) aria-expanded:bg-(--btn-ghost-active-bg) aria-expanded:text-(--btn-ghost-hover-text) focus-visible:bg-(--btn-ghost-focus-bg) focus-visible:text-(--btn-ghost-hover-text) focus-visible:border-(--btn-ghost-focus-border)',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-9 gap-1.5 px-6 in-data-[slot=button-group]:rounded-md has-data-[icon=inline-end]:pr-2 has-data-[icon=inline-start]:pl-2',
        xs: 'h-6 gap-1 px-3 text-xs in-data-[slot=button-group]:rounded-lg has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 [&_svg:not([class*="size-"])]:size-3',
        sm: 'h-7 gap-1 px-4 text-[0.8rem] in-data-[slot=button-group]:rounded-lg has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 [&_svg:not([class*="size-"])]:size-3.5',
        lg: 'h-9 gap-1.5 px-6 has-data-[icon=inline-end]:pr-3 has-data-[icon=inline-start]:pl-3',
        icon: 'size-8',
        'icon-xs': 'size-6 in-data-[slot=button-group]:rounded-lg [&_svg:not([class*="size-"])]:size-3',
        'icon-sm': 'size-7 in-data-[slot=button-group]:rounded-lg',
        'icon-lg': 'size-9',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
);

export type ButtonProps = ButtonPrimitive.Props & VariantProps<typeof buttonVariants> & {
  loading?: boolean,
};

export function Button({
  className,
  variant = 'default',
  size = 'default',
  disabled,
  loading = false,
  children,
  ...props
}: ButtonProps) {
  return (
    <ButtonPrimitive
      data-slot="button"
      data-variant={variant}
      className={cn(buttonVariants({ variant, size, className }))}
      disabled={disabled || loading}
      {...props}
    >
      {loading && (
        <Spinner data-icon="icon-start" />
      )}
      {children}
    </ButtonPrimitive>
  );
}
