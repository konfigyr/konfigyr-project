'use client';

import { useEffect, useEffectEvent, useState } from 'react';
import { Search } from 'lucide-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { Dialog as DialogPrimitive } from 'radix-ui';
import { Button } from '@konfigyr/components/ui/button';
import { Input } from '@konfigyr/components/ui/input';
import { Kbd, KbdGroup } from '@konfigyr/components/ui/kbd';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

function SearchForm(props: ComponentProps<'form'>) {
  const intl = useIntl();

  return (
    <form
      {...props}
      className="flex items-center"
      noValidate
      autoComplete="off"
      onSubmit={event => event.preventDefault()}
    >
      <Search size="1.25rem" />
      <Input
        type="search"
        placeholder={intl.formatMessage({
          defaultMessage: 'Search...',
          description: 'Placeholder for the search input in the search modal dialog',
        })}
        className="border-none shadow-none focus:ring-0 focus-visible:ring-0"
      />
      <DialogPrimitive.Close>
        <span className="sr-only">
          <FormattedMessage
            defaultMessage="Close search"
            description="Label for the close button in the search modal dialog"
          />
        </span>
        <Kbd aria-hidden>ESC</Kbd>
      </DialogPrimitive.Close>
    </form>
  );
}

function SearchDialog(props: ComponentProps<typeof DialogPrimitive.Root>) {
  return (
    <DialogPrimitive.Root {...props}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.DialogOverlay
          data-slot="dialog-overlay"
          className={cn(
            'fixed inset-0 z-50 bg-black/50 backdrop-blur-xs',
            'data-[state=open]:animate-in data-[state=closed]:animate-out',
            'data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
          )}
        />
        <DialogPrimitive.Content
          data-slot="dialog-content"
          className={cn(
            'bg-background fixed top-[50%] left-[50%] z-50 grid w-full max-w-[calc(100%-2rem)] translate-x-[-50%] translate-y-[-50%]',
            'gap-4 rounded-lg border py-2 px-4 shadow-lg duration-200 sm:max-w-lg',
            'data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0',
            'data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95',
          )}
        >
          <DialogPrimitive.Title className="sr-only">
            <FormattedMessage
              defaultMessage="Search"
              description="Title for the search modal dialog"
            />
          </DialogPrimitive.Title>
          <SearchForm />
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}

export function SearchToggle({ className, ...props }: ComponentProps<'button'>) {
  const [open, setOpen] = useState(false);

  const onKeyDown = useEffectEvent((event: KeyboardEvent) => {
    if (event.metaKey && event.key === 'k') {
      setOpen(state => !state);
      event.preventDefault();
    }
  });

  useEffect(() => {
    window.addEventListener('keydown', onKeyDown);

    return () => {
      window.removeEventListener('keydown', onKeyDown);
    };
  });

  return (
    <>
      <Button
        {...props}
        size="sm"
        variant="ghost"
        className={cn(
          'min-w-7 md:min-w-3xs bg-surface text-foreground dark:bg-card relative justify-start pl-3 font-medium md:border',
          className,
        )}
        onClick={() => setOpen(true)}
      >
        <Search size="1rem" />
        <span className="sr-only md:flex text-muted-foreground">
          <FormattedMessage
            defaultMessage="Search..."
            description="Label for the search toggle button"
          />
        </span>
        <div className="absolute top-1.5 right-1.5 hidden gap-1 md:flex">
          <KbdGroup aria-hidden>
            <Kbd className="border">⌘</Kbd>
            <Kbd className="border">K</Kbd>
          </KbdGroup>
        </div>
      </Button>

      <SearchDialog open={open} onOpenChange={setOpen} />
    </>
  );
}
