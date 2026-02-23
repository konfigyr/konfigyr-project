'use client';

import { toast } from 'sonner';
import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import { FormattedMessage } from 'react-intl';
import { CheckIcon, CircleXIcon, PencilIcon, SaveIcon } from 'lucide-react';
import { cva } from 'class-variance-authority';
import { useErrorNotification } from '@konfigyr/components/error';
import {
  useCommitChangeset,
  useDiscardChangeset,
  useRenameChangeset,
} from '@konfigyr/hooks';
import { Button } from '@konfigyr/components/ui/button';
import {
  InlineEdit,
  InlineEditInput,
  InlineEditPlaceholder,
} from '@konfigyr/components/ui/inline-edit';
import { StateLabel } from '@konfigyr/components/vault/properties/state-label';
import { cn } from '@konfigyr/components/utils';

import type { RefObject } from 'react';
import type { VariantProps } from 'class-variance-authority';
import type { ChangesetState } from '@konfigyr/hooks/types';

export const changesetStatusBarVariants = cva(
  'flex items-center gap-4 py-2',
  {
    variants: {
      variant: {
        inline: 'px-4 rounded-lg border border-dashed border-amber-300/50 bg-amber-50/50 dark:bg-amber-950/10 dark:border-amber-800/30',
        sticky: null,
      },
    },
  },
);

export interface ChangesetStatusBarProps {
  changeset: ChangesetState,
}

export function ChangesetStatusBar(props: ChangesetStatusBarProps) {
  const inlineRef = useRef<HTMLDivElement>(null);
  const [isInlineBarHidden, setIsInlineBarHidden] = useState(false);

  useEffect(() => {
    if (!inlineRef.current) {
      return;
    }

    const observer = new IntersectionObserver(([entry]) => {
      setIsInlineBarHidden(!entry.isIntersecting);
    }, { root: null, threshold: 0.1 });

    observer.observe(inlineRef.current);
    return () => observer.disconnect();
  }, []);

  return (
    <>
      <ChangesetStatusBarContents {...props} ref={inlineRef} variant="inline" />
      <StickyChangesetStatusBar isVisible={isInlineBarHidden} {...props} />
    </>
  );
}

export function StickyChangesetStatusBar({ isVisible, ...props }: ChangesetStatusBarProps & { isVisible: boolean }) {
  return (
    <div
      className={cn(
        'fixed bottom-0 left-0 right-0 z-50 transition-all duration-300 ease-out',
        isVisible ? 'translate-y-0 opacity-100' : 'translate-y-full opacity-0 pointer-events-none',
      )}
    >
      <div className="border-t border-border/60 bg-background/95 backdrop-blur-md shadow-[0_-4px_20px_rgba(0,0,0,0.08)]">
        <div className="mx-auto max-w-6xl px-6">
          <ChangesetStatusBarContents {...props} variant="sticky" />
        </div>
      </div>
    </div>
  );
}

function ChangesetName({ changeset, onError }: { changeset: ChangesetState, onError: (error: unknown) => void }) {
  const { mutateAsync: onRenameChangeset } = useRenameChangeset(changeset);

  return (
    <div className="flex items-center gap-2 min-w-0">
      <InlineEdit
        value={changeset.name}
        onChange={onRenameChangeset}
        onError={onError}
      >
        <InlineEditPlaceholder>
          <button
            type="button"
            className="flex items-center gap-1.5 text-xs font-medium text-foreground hover:text-foreground/80 transition-colors group/name truncate max-w-50"
          >
            <SaveIcon className="size-3 text-amber-600 dark:text-amber-400 shrink-0" />
            <span className="truncate">{changeset.name}</span>
            <PencilIcon className="size-2.5 text-muted-foreground/50 opacity-0 group-hover/name:opacity-100 transition-opacity shrink-0" />
          </button>
        </InlineEditPlaceholder>
        <InlineEditInput
          placeholder="Changeset name..."
          className="h-6 text-xs w-48"
        />
      </InlineEdit>
    </div>
  );
}

function CommitButton({ changeset, onError }: { changeset: ChangesetState, onError: (error: unknown) => void }) {
  const { mutateAsync: onCommitChangeset, isPending } = useCommitChangeset();

  const onCommit = useCallback(async () => {
    try {
      await onCommitChangeset(changeset);
    } catch (error) {
      return onError(error);
    }

    return toast.success((
      <FormattedMessage
        defaultMessage="Your changes were successfully commited"
        description="Notification message that is shown when changeset state was commited."
      />
    ));
  }, [changeset]);

  return (
    <Button
      size="sm"
      loading={isPending}
      disabled={isPending}
      onClick={onCommit}
    >
      <CheckIcon />
      <FormattedMessage
        defaultMessage="Commit"
        description="Label for the commit button in the changeset status bar."
      />
    </Button>
  );
}

function DiscardButton({ changeset, onError }: { changeset: ChangesetState, onError: (error: unknown) => void }) {
  const { mutateAsync: onDiscardChangeset, isPending } = useDiscardChangeset();

  const onDiscard = useCallback(async () => {
    try {
      await onDiscardChangeset(changeset);
    } catch (error) {
      return onError(error);
    }

    return toast.success((
      <FormattedMessage
        defaultMessage="Your changes were discarded"
        description="Notification message that is shown when changeset state was discarded."
      />
    ));
  }, [changeset]);

  return (
    <Button
      size="sm"
      variant="outline"
      loading={isPending}
      disabled={isPending}
      onClick={onDiscard}
    >
      <CircleXIcon />
      <FormattedMessage
        defaultMessage="Discard"
        description="Label for the discard button in the changeset status bar."
      />
    </Button>
  );
}

function ChangesetStatusBarContents({
  changeset,
  variant,
  ref,
}: ChangesetStatusBarProps & VariantProps<typeof changesetStatusBarVariants> & { ref?: RefObject<HTMLDivElement | null> }) {
  const errorNotification = useErrorNotification();

  const totalChanges = changeset.added + changeset.modified + changeset.deleted;

  return (
    <div ref={ref} className={changesetStatusBarVariants({ variant })}>
      <ChangesetName
        changeset={changeset}
        onError={errorNotification}/>

      <div className="h-4 w-px bg-border shrink-0" />

      <div className="flex items-center gap-3">
        <p className="text-[11px] text-muted-foreground tabular-nums">
          {totalChanges} {totalChanges === 1 ? 'change' : 'changes'}
        </p>
        <div className="flex items-center gap-2.5">
          {changeset.modified > 0 && (
            <StateLabel variant="modified" count={changeset.modified} />
          )}
          {changeset.added > 0 && (
            <StateLabel variant="added" count={changeset.added} />
          )}
          {changeset.deleted > 0 && (
            <StateLabel variant="deleted" count={changeset.deleted} />
          )}
        </div>
      </div>

      <div className="flex-1" />

      <div className="flex items-center gap-2 shrink-0">
        <DiscardButton
          changeset={changeset}
          onError={errorNotification}
        />
        <CommitButton
          changeset={changeset}
          onError={errorNotification}
        />
      </div>
    </div>
  );
}
