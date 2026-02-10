'use client';

import * as React from 'react';
import { Slot } from 'radix-ui';
import { createFormHook, createFormHookContexts } from '@tanstack/react-form';
import { Button } from '@konfigyr/components/ui/button';
import { Input } from '@konfigyr/components/ui/input';
import { Label } from '@konfigyr/components/ui/label';
import { Switch } from '@konfigyr/components/ui/switch';
import { Textarea } from '@konfigyr/components/ui/textarea';
import { cn } from '@konfigyr/components/utils';

import type { FieldApi } from '@tanstack/react-form';
import type { ButtonProps } from './button';

export type Primitive = string | number | boolean | undefined | null;

/**
 * Type that defines the value of the form control context.
 */
interface FormControlContextValue<in out TData extends | undefined | Primitive> {
  /**
   * The HTML identifier of the form control input element.
   */
  formInputId: string;
  /**
   * The HTML identifier of the form control description element.
   */
  formDescriptionId: string;
  /**
   * The HTML identifier of the error element for the form control.
   */
  formErrorId: string;
  /**
   * The TanStack form field state.
   */
  field: FieldApi<any, string, TData, any, any, any, any, any, any, any, any, any, any, any, any, any, any, any, any, any, any, any, any>;
}

/* Create form and field contexts used to customize the TanStack form */
const { fieldContext, formContext, useFieldContext, useFormContext } = createFormHookContexts();

/* Create a custom form control context to be used by internal components */
const FormControlContext = React.createContext<FormControlContextValue<any>>({} as any);

function useFormControl<TData extends Primitive>(): FormControlContextValue<TData> {
  return React.useContext<FormControlContextValue<TData>>(FormControlContext);
}

export function FormControl<TData extends undefined | Primitive>({
  label,
  description,
  children,
  ...props
}: {
  label?: string | React.ReactNode;
  description?: string | React.ReactNode;
  children?: React.ReactNode
} & Omit<React.ComponentProps<'div'>, 'children'>) {
  const field = useFieldContext<TData>();
  const id = React.useId();

  return (
    <FormControlContext.Provider value={{
      formInputId: id,
      formDescriptionId: `${id}-description`,
      formErrorId: `${id}-error`,
      field,
    }}>
      <FormField
        label={label}
        description={description}
        {...props}
      >
        {children}
      </FormField>
    </FormControlContext.Provider>
  );
}

function FormField<TData extends undefined | Primitive>({ children, label, description, className, ...props }: {
  label?: string | React.ReactNode;
  description?: string | React.ReactNode;
  children: React.ReactNode
} & Omit<React.ComponentProps<'div'>, 'children'>) {
  const { formInputId, formDescriptionId, formErrorId, field } = useFormControl<TData>();
  const ariaDescribedBy = React.useMemo(() => {
    const ids = [];

    if (description) {
      ids.push(formDescriptionId);
    }

    if (!field.state.meta.isValid) {
      ids.push(formErrorId);
    }

    if (ids.length > 0) {
      return ids.join(' ');
    }
  }, [description, formDescriptionId, formErrorId, field.state.meta.isValid]);

  return (
    <div className={cn('grid gap-2', className)} {...props}>
      {label && (
        <Label
          data-error={!field.state.meta.isValid}
          className={cn('data-[error=true]:text-destructive', className)}
          htmlFor={formInputId}
        >
          {label}
        </Label>
      )}

      <Slot.Root
        data-slot="form-control"
        id={formInputId}
        aria-describedby={ariaDescribedBy}
        aria-invalid={!field.state.meta.isValid}
      >
        {children}
      </Slot.Root>

      {description && (
        <FormDescription>
          {description}
        </FormDescription>
      )}

      <FormError />
    </div>
  );
}

export function FormDescription({ className, children, ...props }: React.ComponentProps<'p'>) {
  const { formDescriptionId } = useFormControl();

  return (
    <p id={formDescriptionId} className={cn('text-muted-foreground text-sm', className)} {...props}>
      {children}
    </p>
  );
}

export function FormError({ className, children, ...props }: React.ComponentProps<'p'>) {
  const { formErrorId, field } = useFormControl();
  const errors = field.state.meta.errors;
  let body: React.ReactNode = null;

  if (errors.length > 0) {
    body = errors.map((error, index) => {
      if (error && typeof error === 'object' && 'message' in error) {
        return (
          <span key={error.message}>{error.message}</span>
        );
      }

      return (
        <span key={index}>{error}</span>
      );
    });
  } else if (children) {
    body = children;
  }

  if (body === null) {
    return null;
  }

  return (
    <p id={formErrorId} className={cn('text-destructive text-sm', className)} {...props}>
      {body}
    </p>
  );
}

export function SubmitButton({ children, ...props }: ButtonProps) {
  const form = useFormContext();

  return (
    <form.Subscribe
      selector={state => [state.isValid, state.isValidating, state.isSubmitting]}
      children={([isValid, isValidating, isSubmitting]) => (
        <Button
          type="submit"
          disabled={!isValid}
          loading={isSubmitting || isValidating}
          {...props}
        >
          {children}
        </Button>
      )}
    />
  );
}

export function FormInput(props: React.ComponentProps<typeof Input>) {
  const field = useFieldContext<string>();

  return (
    <Input
      value={field.state.value}
      onChange={event => field.handleChange(event.target.value)}
      onBlur={() => field.handleBlur()}
      {...props}
    />
  );
}

export function FormSwitch(props: React.ComponentProps<typeof Switch>) {
  const { formInputId, field } = useFormControl<boolean>();

  return (
    <Switch
      id={formInputId}
      name={field.name}
      checked={field.state.value}
      onCheckedChange={field.handleChange}
      onBlur={() => field.handleBlur()}
      {...props}
    />
  );
}

export function FormTextarea(props: React.ComponentProps<typeof Textarea>) {
  const field = useFieldContext<string>();

  return (
    <Textarea
      value={field.state.value}
      onChange={event => field.handleChange(event.target.value)}
      onBlur={() => field.handleBlur()}
      {...props}
    />
  );
}

/* Expose the `useForm` hook that provides access to custom field and form components */
export const { useAppForm: useForm } = createFormHook({
  fieldContext,
  formContext,
  fieldComponents: {
    Control: FormControl,
    Input: FormInput,
    Label: Label,
    Switch: FormSwitch,
    Textarea: FormTextarea,
  },
  formComponents: {
    Submit: SubmitButton,
  },
});

export { useFieldContext, useFormContext };
