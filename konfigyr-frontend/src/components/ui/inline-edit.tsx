'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import { Slot } from 'radix-ui';
import { CheckIcon, XIcon } from 'lucide-react';
import { CancelLabel, SaveLabel } from '@konfigyr/components/messages';
import { Button } from '@konfigyr/components/ui/button';
import { Input } from '@konfigyr/components/ui/input';
import { Switch } from '@konfigyr/components/ui/switch';
import { Textarea } from '@konfigyr/components/ui/textarea';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps, KeyboardEvent, ReactNode } from 'react';

type EditingContext<T> = {
  value?: T,
  isEditing: boolean,
  isPending: boolean,
  setValue: (value: T) => void,
  onEdit: () => void,
  onCancel: () => void,
  onSave: () => void,
};

const InlineEditContext = createContext<EditingContext<any>>({
  isEditing: false,
  isPending: false,
  setValue: () => {},
  onEdit: () => {},
  onCancel: () => {},
  onSave: () => {},
});

export function InlineEdit<T>({ value, children, onChange, onError }: {
  value: T, children: ReactNode,
  onChange: (value?: T) => unknown | Promise<unknown>,
  onError?: (error: unknown) => unknown | Promise<unknown>,
}) {
  const [isEditing, setIsEditing] = useState(false);
  const [isPending, setIsPending] = useState(false);
  const [editValue, setEditValue] = useState(value);

  // update the edit value state when the value changes from the outside
  useEffect(() => {
    if (!isEditing) {
      setEditValue(value);
    }
  }, [value, isEditing]);

  const onEdit = useCallback(() => {
    setEditValue(value);
    setIsEditing(true);
  }, [value]);

  const onCancel = useCallback(() => {
    setEditValue(value);
    setIsEditing(false);
  }, [value]);

  const onSave = useCallback(async () => {
    if (editValue !== value) {
      setIsPending(true);

      try {
        await onChange(editValue);
        setIsEditing(false);
      } catch (e) {
        if (typeof onError === 'function') {
          await onError(e);
        } else {
          throw e;
        }
      } finally {
        setIsPending(false);
      }
    } else {
      setIsEditing(false);
    }
  }, [editValue, value, onChange]);

  const context: EditingContext<T> = {
    isEditing,
    isPending,
    value: editValue,
    setValue: setEditValue,
    onEdit,
    onCancel,
    onSave,
  };

  return (
    <InlineEditContext.Provider value={context}>
      {children}
    </InlineEditContext.Provider>
  );
}

export function InlineEditPlaceholder({ disabled = false, className, children }: {
  disabled?: boolean,
  className?: string,
  children?: ReactNode
}) {
  const { isEditing, value, onEdit } = useContext(InlineEditContext);

  if (isEditing) {
    return null;
  }

  const onKeyDown = useCallback((event: KeyboardEvent) => {
    if (event.key === 'Enter') {
      onEdit();
    }
  }, [onEdit]);

  return (
    <Slot.Root
      className={cn('group/inline-edit-placeholder relative cursor-pointer', className)}
      onClick={disabled ? undefined : onEdit}
      onKeyDown={onKeyDown}
      role={disabled ? undefined : 'button'}
      tabIndex={disabled ? undefined : 0}
    >
      {children ? children : (
        <span className="text-muted-foreground">{value}</span>
      )}
    </Slot.Root>
  );
}

const useKeyboardEvents = (context: EditingContext<any>) => useCallback((event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    context.onSave();
  }
  if (event.key === 'Escape') {
    context.onCancel();
  }
}, [context.onCancel, context.onSave]);

function InlineEditContainer<T>({ className, children, ...props }: ComponentProps<'div'>) {
  const { isEditing, isPending, onCancel, onSave }: EditingContext<T> = useContext(InlineEditContext);

  if (!isEditing) {
    return null;
  }

  return (
    <div
      className={cn(
        'group/inline-edit-input flex items-center gap-1.5',
        isPending && 'opacity-80 pointer-events-none',
        className,
      )}
      {...props}
    >
      {children}

      <Button
        variant="ghost"
        size="icon"
        className="size-7 shrink-0 text-des text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 dark:hover:bg-emerald-950"
        onMouseDown={(e) => e.preventDefault()}
        loading={isPending}
        onClick={onSave}
      >
        <CheckIcon />
        <span className="sr-only">
          <SaveLabel />
        </span>
      </Button>
      <Button
        variant="ghost"
        size="icon"
        className="size-7 shrink-0 text-muted-foreground hover:text-foreground"
        onMouseDown={(e) => e.preventDefault()}
        onClick={onCancel}
      >
        <XIcon />
        <span className="sr-only">
          <CancelLabel />
        </span>
      </Button>
    </div>
  );
}

export function InlineEditInput(props: ComponentProps<typeof Input>) {
  const context: EditingContext<string> = useContext(InlineEditContext);
  const ref = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (context.isEditing && ref.current) {
      ref.current.focus();
      ref.current.select();
    }
  }, [context.isEditing]);

  const onKeyDown = useKeyboardEvents(context);

  return (
    <InlineEditContainer>
      <Input
        ref={ref}
        value={context.value}
        onChange={event => context.setValue(event.target.value)}
        onBlur={context.onCancel}
        onKeyDown={onKeyDown}
        {...props}
      />
    </InlineEditContainer>
  );
}

export function InlineEditTextarea(props: ComponentProps<typeof Textarea>) {
  const context: EditingContext<string> = useContext(InlineEditContext);
  const ref = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (context.isEditing && ref.current) {
      ref.current.focus();
      ref.current.select();
    }
  }, [context.isEditing]);

  const onKeyDown = useKeyboardEvents(context);

  return (
    <InlineEditContainer>
      <Textarea
        ref={ref}
        value={context.value}
        onChange={event => context.setValue(event.target.value)}
        onBlur={context.onCancel}
        onKeyDown={onKeyDown}
        {...props}
      />
    </InlineEditContainer>
  );
}

export function InlineEditSwitch(props: ComponentProps<typeof Switch>) {
  const context: EditingContext<boolean> = useContext(InlineEditContext);
  const onKeyDown = useKeyboardEvents(context);

  return (
    <InlineEditContainer>
      <Switch
        checked={context.value}
        onCheckedChange={context.setValue}
        onBlur={context.onCancel}
        onKeyDown={onKeyDown}
        {...props}
      />
    </InlineEditContainer>
  );
}
