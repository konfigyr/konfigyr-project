import { useCallback, useEffect, useState } from 'react';
import { ClipboardCheckIcon, ClipboardCopyIcon } from 'lucide-react';
import { useClipboard } from '@konfigyr/hooks/clipboard';
import { Button } from '@konfigyr/components/ui/button';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';
import { cn } from '@konfigyr/components/utils';
import { CopiedLabel, CopyLabel } from '@konfigyr/components/messages';

import type { ComponentProps } from 'react';

function useClipboardButton(text: string): [boolean, () => void] {
  const [copied, setCopied] = useState(false);

  const clipboard = useClipboard({
    onCopy: () => setCopied(true),
  });

  useEffect(() => {
    if (copied) {
      const timeout = setTimeout(() => setCopied(false), 1000);
      return () => clearTimeout(timeout);
    }
  }, [copied]);

  const onCopy = useCallback(() => clipboard(text), [clipboard, text]);

  return [copied, onCopy];
}

export type ClipboardButtonProps = { text: string } & ComponentProps<typeof Button>;

export function ClipboardButton({ text, className, children, ...props }: ClipboardButtonProps) {
  const [copied, onCopy] = useClipboardButton(text);

  const body = children || (
    <>
      <ClipboardCopyIcon />
      <CopyLabel />
    </>
  );

  return (
    <Button
      {...props}
      className={cn(copied && 'bg-primary-foreground! text-primary!', className)}
      onClick={onCopy}
    >
      {copied ? (
        <>
          <ClipboardCheckIcon />
          <CopiedLabel />
        </>
      ) : body}
    </Button>
  );
}

export function ClipboardIconButton({
  text,
  delayDuration,
  className,
  children,
  ...props
}: { delayDuration?: number } & ClipboardButtonProps) {
  const [copied, onCopy] = useClipboardButton(text);
  const [open, setOpen] = useState(false);

  const body = children || (<ClipboardCopyIcon />);

  return (
    <Tooltip
      open={copied || open}
      delayDuration={delayDuration}
      onOpenChange={setOpen}
    >
      <TooltipTrigger asChild>
        <Button
          {...props}
          className={cn(copied && 'bg-primary-foreground! text-primary!', className)}
          onClick={onCopy}
        >
          {copied ? (<ClipboardCheckIcon />) : body}
        </Button>
      </TooltipTrigger>
      <TooltipContent>
        {copied ? (<CopiedLabel />) : (<CopyLabel />)}
      </TooltipContent>
    </Tooltip>
  );
}
