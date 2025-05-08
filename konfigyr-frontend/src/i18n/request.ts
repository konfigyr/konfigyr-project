import type { IntlConfig } from 'use-intl/core';
import { getRequestConfig } from 'next-intl/server';
import { Locale } from './index';
import messages from '../../messages/en.json';

/**
 * Function that would return the resolve the current locale context.
 * <p>
 * For the moment there is only one supported locale, {@link Locale#ENGLISH}. Therefore, the
 * translations messages is preloaded and just exported as config.
 *
 * @return {IntlConfig} configuration for Next Intl plugin
 */
const localeContextResolver = (): IntlConfig => ({
  locale: Locale.ENGLISH,
  messages: messages,
});

export default getRequestConfig(async () => await localeContextResolver());
