import { mergeProps } from '@base-ui/react/merge-props';
import { useRender } from '@base-ui/react/use-render';
import { cva } from 'class-variance-authority';
import { cn } from '@konfigyr/components/utils';

import type { VariantProps } from 'class-variance-authority';

export const badgeVariants = cva(
  'group/badge inline-flex h-5 w-fit shrink-0 items-center justify-center gap-1 overflow-hidden rounded-4xl border border-transparent px-2 py-0.5 text-xs font-semibold whitespace-nowrap transition-all focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&>svg]:pointer-events-none [&>svg]:size-3',
  {
    variants: {
      variant: {
        default:
          'bg-primary text-primary-foreground [a]:hover:bg-primary/80',
        secondary:
          'bg-secondary text-secondary-foreground [a]:hover:bg-secondary/80',
        info:
          'border-info bg-info/10 text-info focus-visible:ring-info/20 dark:bg-info/20 dark:focus-visible:ring-info/40 [a]:hover:bg-info/20',
        success:
          'border-success bg-success/10 text-success focus-visible:ring-success/20 dark:bg-success/20 dark:focus-visible:ring-success/40 [a]:hover:bg-success/20',
        warning:
          'border-warning bg-warning/10 text-warning focus-visible:ring-warning/20 dark:bg-warning/20 dark:focus-visible:ring-warning/40 [a]:hover:bg-warning/20',
        destructive:
          'border-destructive bg-destructive/10 text-destructive focus-visible:ring-destructive/20 dark:bg-destructive/20 dark:focus-visible:ring-destructive/40 [a]:hover:bg-destructive/20',
        outline:
          'border-border text-foreground [a]:hover:bg-muted [a]:hover:text-muted-foreground',
        ghost:
          'hover:bg-muted hover:text-muted-foreground dark:hover:bg-muted/50',
      },
      size: {
        xs: 'h-5 px-2 py-0 text-[10px] leading-none',
        sm: 'h-6 px-2.5 py-0 text-xs leading-none',
        default: 'h-auto px-3 py-1 text-[13px] leading-5',
        lg: 'h-auto px-4 py-1.5 text-sm leading-5',
        xl: 'h-auto px-5 py-2 text-base leading-5',
        '2xl': 'h-auto px-6 py-2.5 text-lg leading-5',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
);

export type BadgeProps = useRender.ComponentProps<'span'> & VariantProps<typeof badgeVariants>;

export function Badge({
  className,
  variant = 'default',
  size = 'default',
  render,
  ...props
}: BadgeProps) {
  return useRender({
    defaultTagName: 'span',
    props: mergeProps<'span'>({
      className: cn(badgeVariants({ variant, size, className })),
    }, props),
    render,
    state: {
      slot: 'badge',
      variant,
      size,
    },
  });
}
