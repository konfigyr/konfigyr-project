import { FormattedMessage } from 'react-intl';
import { cn } from '@konfigyr/ui/lib/utils';

import type { ComponentProps } from 'react';
import type { MessageDescriptor } from 'react-intl';

export interface ScreenshotPlaceholderProps extends Omit<ComponentProps<'figure'>, 'children'> {
  description: MessageDescriptor;
}

export function ScreenshotPlaceholder({ description, className, ...props }: ScreenshotPlaceholderProps) {
  return (
    <figure
      data-slot="screenshot-placeholder"
      className={cn(
        'aspect-video w-full rounded-lg border-2 border-dashed border-warning/60 bg-warning/10',
        'flex flex-col items-center justify-center gap-3 p-6 text-center',
        className,
      )}
      {...props}
    >
      <svg
        aria-hidden="true"
        focusable="false"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="size-8 text-warning-foreground"
      >
        <rect x="3" y="4" width="18" height="16" rx="2" />
        <circle cx="8.5" cy="9.5" r="1.5" />
        <path d="m4 17 5-5 4 4 3-3 4 4" />
      </svg>

      <figcaption className="grid gap-1 max-w-lg">
        <span className="text-xs font-semibold uppercase tracking-wide text-warning-foreground">
          <FormattedMessage
            defaultMessage="Screenshot placeholder: not yet added"
            description="Badge label on an unresolved marketing screenshot placeholder, flagging that a real screenshot still needs to be captured and dropped in"
          />
        </span>
        <span className="text-sm text-muted-foreground">
          <FormattedMessage {...description} />
        </span>
      </figcaption>
    </figure>
  );
}
