import { useMemo } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

export function TextStats({ text = '', className, ...props }: { text?: string } & ComponentProps<'div'>) {
  const intl = useIntl();

  const { chars, lines } = useMemo(() => ({
    chars: text.length,
    lines: text.split('\n').length,
  }), [text]);

  return (
    <div
      className={cn('flex items-center gap-2 font-mono select-none', className)}
      {...props}
    >
      <span>
        {intl.formatMessage({
          defaultMessage: '{lines, plural, one {1 line} other {# lines}}',
          description: 'Label for the number of lines in the editor',
        }, { lines })}
      </span>
      <span>
        {intl.formatMessage({
          defaultMessage: '{chars, plural, one {1 char} other {# chars}}',
          description: 'Label for the number of characters in the editor',
        }, { chars })}
      </span>
    </div>
  );
}

export function MarkdownSupported({ className, ...props }: { className?: string } & ComponentProps<'div'>) {
  return (
    <div className={cn('flex items-center gap-1 text-xs', className)} {...props}>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        className="size-4"
        fill="currentColor"
        aria-hidden="true"
      >
        <title>Markdown</title>
        <path d="M22.27 19.385H1.73A1.73 1.73 0 010 17.655V6.345a1.73 1.73 0 011.73-1.73h20.54A1.73 1.73 0 0124 6.345v11.308a1.73 1.73 0 01-1.73 1.731zM5.769 15.923v-4.5l2.308 2.885 2.307-2.885v4.5h2.308V8.078h-2.308l-2.307 2.885-2.308-2.885H3.46v7.847zM21.232 12h-2.309V8.077h-2.307V12h-2.308l3.461 4.039z"/>
      </svg>
      <FormattedMessage
        defaultMessage="Markdown supported"
        description="Label for the markdown editor that informs the user that markdown is supported"
      />
    </div>
  );
}

export function StatusBar({ value, className, ...props }: { value?: string, className?: string } & ComponentProps<'div'>) {
  return (
    <div
      data-slot="editor-statusbar"
      className={cn(
        'flex items-center justify-between px-2.5 py-1 text-xs text-muted-foreground',
        className,
      )}
      {...props}
    >
      <MarkdownSupported />
      <TextStats text={value} />
    </div>
  );
}
