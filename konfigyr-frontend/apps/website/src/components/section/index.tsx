import { mergeProps } from '@base-ui/react/merge-props';
import { useRender } from '@base-ui/react/use-render';
import { cn } from '@konfigyr/ui/lib/utils';
import { cva } from 'class-variance-authority';

import type { ComponentProps, ReactNode } from 'react';
import type { VariantProps } from 'class-variance-authority';

export const sectionVariants = cva(
  'group/section px-4 py-16',
  {
    variants: {
      variant: {
        default: '',
        secondary: 'bg-muted/50',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
);

export type SectionProps = VariantProps<typeof sectionVariants> & ComponentProps<'section'>;

export function Section({ children, variant, className, ...props }: SectionProps) {
  return (
    <section
      data-slot="section"
      className={cn(sectionVariants({ variant }), className)}
      {...props}
    >
      <div className="container max-w-3xl mx-auto">
        {children}
      </div>
    </section>
  );
}

export function SectionHeader({ children, className, ...props }: { children: ReactNode } & ComponentProps<'header'>) {
  return (
    <header
      data-slot="section-header"
      className={cn('mb-10', className)}
      {...props}
    >
      {children}
    </header>
  );
}

export function SectionTitle({ render, className, ...props }: useRender.ComponentProps<'h2'>) {
  return useRender({
    defaultTagName: 'h2',
    props: mergeProps<'h2'>({
      className: cn(
        'mt-3 font-heading text-[clamp(1.7rem,3.4vw,2.5rem)] font-extrabold tracking-tight text-balance',
        className,
      ),
    }, props),
    render,
    state: {
      slot: 'section-title',
    },
  });
}

export function SectionDescription({ children, className, ...props }: { children: ReactNode } & ComponentProps<'p'>) {
  return (
    <p
      data-slot="section-description"
      className={cn('mt-4 text-lg text-muted-foreground', className)}
      {...props}
    >
      {children}
    </p>
  );
}

export function SectionContent({ children, className, ...props }: { children: ReactNode } & ComponentProps<'div'>) {
  return (
    <div
      data-slot="section-content"
      className={cn('grid gap-4', className)}
      {...props}
    >
      {children}
    </div>
  );
}
