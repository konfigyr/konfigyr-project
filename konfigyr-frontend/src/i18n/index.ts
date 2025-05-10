import type { IntlConfig } from 'use-intl/core';
import messages from '../../messages/en.json';
import assert from "node:assert";

/**
 * Enumeration that contains the supported locales.
 */
export enum Locale {
  ENGLISH = 'en',
}

const MESSAGES = {
  [Locale.ENGLISH]: messages,
}

/**
 * Function that would return the resolve the current locale context.
 * <p>
 * For the moment there is only one supported locale, {@link Locale#ENGLISH}. Therefore, the
 * translations messages is preloaded and just exported as config.
 *
 * @return {IntlConfig} configuration for Next Intl plugin
 */
export function resolve(locale: Locale = Locale.ENGLISH): IntlConfig {
  assert(Object.values(Locale).includes(locale), `Unsupported locale of: '${locale}'.`);

  return { locale, messages: MESSAGES[locale] };
}
