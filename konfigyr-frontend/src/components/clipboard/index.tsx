import { useEffect, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { ClipboardCheckIcon, ClipboardCopyIcon } from 'lucide-react';
import { useClipboard } from '@konfigyr/hooks/clipboard';
import { Button } from '@konfigyr/components/ui/button';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

export function ClipboardButton({ text, className, children, ...props }: { text: string } & ComponentProps<typeof Button>) {
  const [copied, setCopied] = useState(false);
  const clipboard = useClipboard({
    onCopy: () => setCopied(true),
  });

  const body = children || (
    <>
      <ClipboardCopyIcon />
      <FormattedMessage
        defaultMessage="Copy"
        description="Default label for the clipboard button, usually used to copy some text to the clipboard."
      />
    </>
  );

  useEffect(() => {
    if (copied) {
      const timeout = setTimeout(() => setCopied(false), 1000);
      return () => clearTimeout(timeout);
    }
  }, [copied]);

  return (
    <Button
      {...props}
      className={cn(copied && 'bg-primary-foreground! text-primary!', className)}
      onClick={() => clipboard(text)}
    >
      {copied ? (
        <>
          <ClipboardCheckIcon />
          <FormattedMessage
            defaultMessage="Copied!"
            description="Label for the clipboard button notifying the user that the text has been successfully added to the clipboard."
          />
        </>
      ) : body}
    </Button>
  );
}
