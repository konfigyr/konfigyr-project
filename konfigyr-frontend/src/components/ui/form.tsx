'use client';

import * as React from 'react';
import { Slot } from '@radix-ui/react-slot';
import {
  Controller,
  FormProvider as Form,
  useFormContext,
  useFormState,
  useForm,
  type ControllerProps,
  type FieldPath,
  type FieldValues,
} from 'react-hook-form';
import { cn } from 'konfigyr/components/utils';
import { Label } from './label';

/**
 * Type that defines the value of the form control context.
 */
interface FormControlContextValue<
  Values extends FieldValues = FieldValues,
  Name extends FieldPath<Values> = FieldPath<Values>,
> {
  id: string;
  name: Name;
}

interface FormFieldProps {
  label?: string | React.ReactNode;
  description?: string | React.ReactNode;
  children?: React.ReactNode;
}

const FormControlContext = React.createContext<FormControlContextValue>(
  {} as FormControlContextValue,
);

const useFormControl = () => {
  const context = React.useContext(FormControlContext);

  if (!context) {
    throw new Error('useFormControl should be used within <FormControl>');
  }

  const { getFieldState } = useFormContext();
  const formState = useFormState({ name: context.name });
  const fieldState = getFieldState(context.name, formState);

  return {
    id: context.id,
    name: context.name,
    formItemId: `${context.id}-control-item`,
    formDescriptionId: `${context.id}-form-control-description`,
    formErrorId: `${context.id}-form-control-error`,
    ...fieldState,
  };
};

export const FormControl = <
  Values extends FieldValues = FieldValues,
  Name extends FieldPath<Values> = FieldPath<Values>,
>(props: ControllerProps<Values, Name>) => {
  const id = React.useId();

  return (
    <FormControlContext.Provider value={{ id, name: props.name }}>
      <Controller {...props} />
    </FormControlContext.Provider>
  );
};

export function FormDescription({ className, children, ...props }: React.ComponentProps<'p'>) {
  const { formDescriptionId } = useFormControl();

  return (
    <p id={formDescriptionId} className={cn('text-muted-foreground text-sm', className)} {...props}>
      {children}
    </p>
  );
}

export function FormError({ className, ...props }: React.ComponentProps<'p'>) {
  const { error, formErrorId } = useFormControl();
  const body = error ? String(error?.message ?? '') : props.children;

  if (!body) {
    return null;
  }

  return (
    <p id={formErrorId} className={cn('text-destructive text-sm', className)} {...props}>
      {body}
    </p>
  );
}

export function FormField({ label, description, className, children, ...props }: FormFieldProps & React.ComponentProps<'div'>) {
  const { error, formItemId, formDescriptionId, formErrorId } = useFormControl();

  return (
    <div className={cn('grid gap-2', className)} {...props}>
      {label && (
        <Label
          data-error={!!error}
          className={cn('data-[error=true]:text-destructive', className)}
          htmlFor={formItemId}
        >
          {label}
        </Label>
      )}

      <Slot
        data-slot="form-control"
        id={formItemId}
        aria-describedby={!error ? `${formDescriptionId}` : `${formDescriptionId} ${formErrorId}`}
        aria-invalid={!!error}
      >
        {children}
      </Slot>

      {description && (
        <FormDescription>
          {description}
        </FormDescription>
      )}

      <FormError />
    </div>
  );
}

export {
  Form,
  useForm,
  useFormControl,
};
