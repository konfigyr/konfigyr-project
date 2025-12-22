import { IntlProvider } from 'react-intl';
import defaultMessages from '@konfigyr/translations/en.json';
import { render } from '@testing-library/react';

import type { ReactNode } from 'react';

export function MessagesProvider({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <IntlProvider locale="en" messages={defaultMessages}>
      {children}
    </IntlProvider>
  );
}

export function renderWithMessageProvider(ui: ReactNode) {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MessagesProvider>{children}</MessagesProvider>
  );

  return render(ui, { wrapper });
}
