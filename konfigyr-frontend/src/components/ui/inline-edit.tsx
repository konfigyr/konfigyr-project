'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import { mergeProps } from '@base-ui/react/merge-props';
import { useRender } from '@base-ui/react/use-render';
import { AlertCircleIcon, CheckIcon, XIcon } from 'lucide-react';
import { CancelLabel, SaveLabel } from '@konfigyr/components/messages';
import { Button } from '@konfigyr/components/ui/button';
import { Input } from '@konfigyr/components/ui/input';
import { Switch } from '@konfigyr/components/ui/switch';
import { Textarea } from '@konfigyr/components/ui/textarea';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps, KeyboardEvent, ReactNode, RefObject } from 'react';

type EditingContext<T> = {
  value?: T,
  isEditing: boolean,
  isPending: boolean,
  errors?: Array<string>,
  setValue: (value: T) => void,
  setErrors: (errors: Array<string> | undefined) => void,
  setValidate: (fn: ((value: T) => Array<string> | undefined) | undefined) => void,
  onEdit: () => void,
  onCancel: () => void,
  onSave: () => void,
};

const InlineEditContext = createContext<EditingContext<any>>({
  isEditing: false,
  isPending: false,
  setValue: () => {},
  setErrors: () => {},
  setValidate: () => {},
  onEdit: () => {},
  onCancel: () => {},
  onSave: () => {},
});

export function useInlineEdit<T>() {
  return useContext(InlineEditContext) as EditingContext<T>;
}

export function InlineEdit<T>({ value, children, onChange, onError }: {
  value: T, children: ReactNode,
  onChange: (value?: T) => unknown | Promise<unknown>,
  onError?: (error: unknown) => unknown | Promise<unknown>,
}) {
  const [isEditing, setIsEditing] = useState(false);
  const [isPending, setIsPending] = useState(false);
  const [editValue, setEditValue] = useState(value);
  const [errors, setErrors] = useState<Array<string> | undefined>(undefined);
  const validateRef = useRef<((value: T) => string[] | undefined) | undefined>(undefined);

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
    setErrors(undefined);
  }, [value]);

  const onSave = useCallback(async () => {
    if (validateRef.current) {
      const validationErrors = validateRef.current(editValue);
      if (validationErrors && validationErrors.length > 0) {
        setErrors(validationErrors);
        return;
      }
      setErrors(undefined);
    }

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

  const setValidate = useCallback((fn: ((value: T) => Array<string> | undefined) | undefined) => {
    validateRef.current = fn;
  }, []);

  const context: EditingContext<T> = {
    isEditing,
    isPending,
    errors,
    value: editValue,
    setValue: setEditValue,
    setErrors,
    setValidate,
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

export function InlineEditPlaceholder({ disabled = false, render, className, children }: {
  disabled?: boolean,
  className?: string,
  children?: ReactNode
} & useRender.ComponentProps<'div'>) {
  const { isEditing, value, onEdit } = useContext(InlineEditContext);

  if (isEditing) {
    return null;
  }

  const onKeyDown = useCallback((event: KeyboardEvent) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      event.stopPropagation();

      onEdit();
    }
  }, [onEdit]);

  return useRender({
    defaultTagName: 'div',
    props: mergeProps(
      {
        className: cn('group/inline-edit-placeholder relative cursor-pointer', className),
        onKeyDown,
        role: disabled ? undefined : 'button',
        tabIndex: disabled ? undefined : 0,
      },
      disabled ? {
        'aria-disabled': true,
      } : {
        role: 'button',
        tabIndex: 0,
        onClick: onEdit,
      },
      children ? {
        children,
      } : {
        children: (
          <span className="text-muted-foreground">{value}</span>
        ),
      },
    ),
    render,
    state: {
      slot: 'inline-edit-placeholder',
    },
  });
}

export function useFocusEffect<T extends HTMLElement>(context: EditingContext<any>): RefObject<T | null> {
  const ref = useRef<T>(null);

  useEffect(() => {
    if (context.isEditing && ref.current) {
      ref.current.focus();

      if ('select' in ref.current && typeof ref.current.select === 'function') {
        ref.current.select();
      }
    }
  }, [context.isEditing]);

  return ref;
}

export const useKeyboardEvents = (context: EditingContext<any>) => useCallback((event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    context.onSave();
  }
  if (event.key === 'Escape') {
    context.onCancel();
  }
}, [context.onCancel, context.onSave]);

function InlineEditErrors({ errors }: { errors: Array<string> }) {
  return (
    <Tooltip>
      <TooltipTrigger>
        <span className="flex items-center text-destructive cursor-default">
          <AlertCircleIcon className="size-4" />
        </span>
      </TooltipTrigger>

      <TooltipContent
        side="left"
        className="bg-destructive text-destructive-foreground"
      >
        <ul className="list-disc pl-3 space-y-1">
          {errors.map((msg, i) => (
            <li key={i}>{msg}</li>
          ))}
        </ul>
      </TooltipContent>
    </Tooltip>
  );
}

export function InlineEditContainer<T>({ className, children, ...props }: ComponentProps<'div'>) {
  const { isEditing, isPending, errors, onCancel, onSave }: EditingContext<T> = useContext(InlineEditContext);

  if (!isEditing) {
    return null;
  }

  return (
    <span
      className={cn(
        'group/inline-edit-input flex items-center gap-1.5',
        isPending && 'opacity-80 pointer-events-none',
        className,
      )}
      {...props}
    >
      <span
        className={cn(
          'relative flex items-center gap-1.5',
          errors?.length && 'pl-7',
        )}
      >
        {children}
        {errors?.length ? (
          <span className="absolute left-0 top-1/2 -translate-y-1/2">
            <InlineEditErrors errors={errors}/>
          </span>
        ) : null}
      </span>

      <Button
        variant="ghost"
        size="icon"
        className="size-7 shrink-0 text-success hover:text-success/80 hover:bg-success/10 dark:hover:bg-success/20"
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
        className="size-7 shrink-0 text-destructive hover:text-destructive/80 hover:bg-destructive/10 dark:hover:bg-destructive/20"
        onMouseDown={(e) => e.preventDefault()}
        onClick={onCancel}
      >
        <XIcon />
        <span className="sr-only">
          <CancelLabel />
        </span>
      </Button>
    </span>
  );
}

export function InlineEditInput(props: ComponentProps<typeof Input>) {
  const context: EditingContext<string> = useContext(InlineEditContext);
  const ref = useFocusEffect<HTMLInputElement>(context);
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
  const ref = useFocusEffect<HTMLTextAreaElement>(context);
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
