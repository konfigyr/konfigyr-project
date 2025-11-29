import { createContext, useContext } from 'react';
import type { Account } from '@konfigyr/hooks/types';

export const AccountContext = createContext<Account | null>(null);

export const useAccountContext = () => useContext(AccountContext);
