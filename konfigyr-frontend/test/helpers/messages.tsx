import { IntlProvider } from 'react-intl';
import defaultMessages from '@konfigyr/translations/en.json';

import type { ReactNode } from 'react';

export function MessagesProvider({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <IntlProvider locale="en" messages={defaultMessages}>
      {children}
    </IntlProvider>
  );
}
