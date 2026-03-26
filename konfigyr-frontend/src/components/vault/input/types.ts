import type { FocusEvent, KeyboardEvent, ReactNode, RefObject } from 'react';
import type { PropertyDescriptor } from '@konfigyr/hooks/types';

export interface SchemaHint {
  value: string;
  label: ReactNode;
}

/**
 * Props for the input field component used to edit a configuration property value inside the
 * Konfigyr Vault.
 *
 * @param TElement the type of the HTML element that the input field is rendered into
 * @param TValue the type of the value that the input field can hold
 */
export interface InputFieldProps<TElement extends HTMLElement, TValue> {
  /**
   * Property descriptor of the configuration property to be edited.
   */
  property: PropertyDescriptor,

  /**
   * The current value of the configuration property.
   */
  value?: TValue,

  /**
   * Callback function to be called when the value of the configuration property changes.
   * @param value
   */
  onChange?: (value: TValue) => void,

  /**
   * The list of schema hints to be displayed in the input field.
   */
  hints?: Array<SchemaHint>,

  /**
   * The reference to the HTML element that the input field is rendered into.
   */
  ref?: RefObject<TElement>,

  /**
   * Callback function to be called when a key is pressed inside the input field.
   *
   * @param event
   */
  onKeyDown?: (event: KeyboardEvent<TElement>) => void,

  /**
   * Callback function to be called when the input field gains focus.
   *
   * @param event
   */
  onFocus?: (event?: FocusEvent<TElement>) => void,

  /**
   * Callback function to be called when the input field loses focus. Useful for
   * validation purposes or by the inline edit component.
   *
   * @param event
   */
  onBlur?: (event?: FocusEvent<TElement>) => void,
}
