'use client';

import { toast } from 'sonner';
import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import { FormattedMessage } from 'react-intl';
import { CircleXIcon, PencilIcon, SaveIcon } from 'lucide-react';
import { cva } from 'class-variance-authority';
import {
  useApplyChangeset,
  useDiscardChangeset,
  useRenameChangeset,
  useSubmitChangeset,
} from '@konfigyr/hooks';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { useErrorNotification } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
import {
  InlineEdit,
  InlineEditInput,
  InlineEditPlaceholder,
} from '@konfigyr/components/ui/inline-edit';
import {
  ChangesCountLabel,
  useLabelForTransitionType,
} from '@konfigyr/components/vault/messages';
import { cn } from '@konfigyr/components/utils';
import { ChangesetSubmitDialog } from './dialog';

import type { RefObject } from 'react';
import type { VariantProps } from 'class-variance-authority';
import type { ChangeRequest, ChangesetState } from '@konfigyr/hooks/types';

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
  onChangesetApplied?: (changeset: ChangesetState) => void | Promise<void>,
  onChangesetDiscarded?: (changeset: ChangesetState) => void | Promise<void>,
  onChangesetRenamed?: (changeset: ChangesetState) => void | Promise<void>,
  onChangeRequestCreated?: (changeRequest: ChangeRequest) => void | Promise<void>,
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
      aria-hidden={!isVisible}
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

function ChangesetName({ changeset, onRenamed, onError }: {
  changeset: ChangesetState;
  onError: (error: unknown) => void;
  onRenamed?: ChangesetStatusBarProps['onChangesetRenamed'],
}) {
  const { mutateAsync: onRenameChangeset } = useRenameChangeset(changeset);

  const onRename = useCallback(async (value?: string) => {
    const updated = await onRenameChangeset(value);
    await onRenamed?.(updated);
  }, [changeset]);

  return (
    <div className="flex items-center gap-2 min-w-0">
      <InlineEdit
        value={changeset.name}
        onChange={onRename}
        onError={onError}
      >
        <InlineEditPlaceholder
          render={
            <button
              type="button"
              className="flex items-center gap-1.5 text-xs font-medium text-foreground hover:text-foreground/80 transition-colors group/name truncate max-w-50"
            >
              <SaveIcon className="size-3 text-amber-600 dark:text-amber-400 shrink-0" />
              <span className="truncate">{changeset.name}</span>
              <PencilIcon className="size-2.5 text-muted-foreground/50 opacity-0 group-hover/name:opacity-100 transition-opacity shrink-0" />
            </button>
          }
        />
        <InlineEditInput
          placeholder="Changeset name..."
          className="h-6 text-xs w-48"
        />
      </InlineEdit>
    </div>
  );
}

function StateCountLabel({ type, count }: { type: PropertyTransitionType, count: number }) {
  const label = useLabelForTransitionType(type);

  return (
    <p
      className={cn(
        'flex items-center gap-1 text-xs',
        type === PropertyTransitionType.ADDED && '[&>*:first-child]:bg-emerald-600',
        type === PropertyTransitionType.UPDATED && '[&>*:first-child]:bg-amber-600',
        type === PropertyTransitionType.REMOVED && '[&>*:first-child]:bg-destructive',
      )}
    >
      <span className="size-2 rounded-sm shrink-0" />
      {count && (
        <span className="text-foreground font-medium tabular-nums">{count}</span>
      )}
      <span className="text-muted-foreground hidden sm:inline">
        {label}
      </span>
    </p>
  );
}

function DiscardButton({ changeset, onDiscarded, onError }: {
  changeset: ChangesetState;
  onError: (error: unknown) => void;
  onDiscarded?: ChangesetStatusBarProps['onChangesetDiscarded'];
}) {
  const { mutateAsync: onDiscardChangeset, isPending } = useDiscardChangeset();

  const onDiscard = useCallback(async () => {
    try {
      await onDiscardChangeset(changeset);
    } catch (error) {
      return onError(error);
    }

    await onDiscarded?.(changeset);

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
  onChangesetApplied,
  onChangesetDiscarded,
  onChangesetRenamed,
  onChangeRequestCreated,
}: ChangesetStatusBarProps & VariantProps<typeof changesetStatusBarVariants> & { ref?: RefObject<HTMLDivElement | null> }) {
  const [opened, setOpened] = useState(false);
  const errorNotification = useErrorNotification();

  const { mutateAsync: onApplyChangeset } = useApplyChangeset();
  const { mutateAsync: onSubmitChangeset } = useSubmitChangeset();

  const onApply = useCallback(async (payload: ChangesetState) => {
    try {
      await onApplyChangeset(payload);
    } catch (error) {
      return errorNotification(error);
    }

    setOpened(false);

    await onChangesetApplied?.(changeset);

    return toast.success((
      <FormattedMessage
        defaultMessage="Your changes were successfully applied"
        description="Notification message that is shown when changeset state was applied."
      />
    ));
  }, []);

  const onSubmit = useCallback(async (payload: ChangesetState) => {
    let changeRequest: ChangeRequest;

    try {
      changeRequest = await onSubmitChangeset(payload);
    } catch (error) {
      return errorNotification(error);
    }

    setOpened(false);

    onChangeRequestCreated?.(changeRequest);

    toast.success((
      <FormattedMessage
        defaultMessage="Change request created"
        description="Notification message subject that is shown when changeset state was submitted and change request is created."
      />
    ), {
      description: (
        <FormattedMessage
          defaultMessage="Your changes were submitted for review and are now waiting for approval."
          description="Notification message details that is shown when changeset state was submitted and change request is created."
        />
      ),
    });
  }, []);

  const totalChanges = changeset.added + changeset.modified + changeset.deleted;

  return (
    <div ref={ref} className={changesetStatusBarVariants({ variant })}>
      <ChangesetName
        changeset={changeset}
        onError={errorNotification}
        onRenamed={onChangesetRenamed}
      />

      <div className="h-4 w-px bg-border shrink-0" />

      <div className="flex items-center gap-3">
        <p className="text-[11px] text-muted-foreground tabular-nums">
          <ChangesCountLabel count={totalChanges} />
        </p>
        <div className="flex items-center gap-2.5">
          {changeset.modified > 0 && (
            <StateCountLabel type={PropertyTransitionType.UPDATED} count={changeset.modified} />
          )}
          {changeset.added > 0 && (
            <StateCountLabel type={PropertyTransitionType.ADDED} count={changeset.added} />
          )}
          {changeset.deleted > 0 && (
            <StateCountLabel type={PropertyTransitionType.REMOVED} count={changeset.deleted} />
          )}
        </div>
      </div>

      <div className="flex-1" />

      <div className="flex items-center gap-2 shrink-0">
        <DiscardButton
          changeset={changeset}
          onError={errorNotification}
          onDiscarded={onChangesetDiscarded}
        />

        <ChangesetSubmitDialog
          changeset={changeset}
          disabled={totalChanges === 0}
          open={opened}
          onApply={onApply}
          onSubmit={onSubmit}
          onOpenChange={setOpened}
          onError={errorNotification}
        />
      </div>
    </div>
  );
}
