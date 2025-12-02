import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { MessagesProvider } from './messages';

import type { ReactNode } from 'react';

export const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

export function renderWithQueryClient(ui: ReactNode, { queryClient }: { queryClient?: QueryClient} = {}) {
  const client = queryClient ?? createTestQueryClient();

  const wrapper = ({ children }: { children: ReactNode }) => (
    <MessagesProvider>
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    </MessagesProvider>
  );

  return {
    ...render(ui, { wrapper }),
    queryClient,
  };
}
