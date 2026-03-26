import { mergeProps } from '@base-ui/react/merge-props';
import { useRender } from '@base-ui/react/use-render';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

export function TabItem({ render, className, ...props }: useRender.ComponentProps<'button'> & ComponentProps<'button'>) {
  return useRender({
    defaultTagName: 'button',
    props: mergeProps(
      {
        role: 'tab',
        className: cn(
          'inline-flex items-center justify-center gap-1.5 px-2 py-1 text-sm font-medium whitespace-nowrap transition',
          'bg-background text-foreground/80 hover:text-foreground dark:text-muted-foreground dark:hover:text-foreground',
          'data-[state=active]:text-foreground dark:data-[state=active]:text-foreground',
          'data-[state=active]:border-primary border-b-2 border-transparent',
          'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:outline-ring focus-visible:ring-[3px] focus-visible:outline-1',
          '[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*="size-"])]:size-4',
          className,
        ),
      },
      props,
    ),
    render,
    state: {
      slot: 'tab-item',
    },
  });
}

export function Tabs({ className, children, ...props }: ComponentProps<'div'>) {
  return (
    <div
      role="tablist"
      data-slot="tab-container"
      className={cn(className, 'text-muted-foreground flex w-full gap-2 bg-background border-b')}
      {...props}
    >
      {children}
    </div>
  );
}
