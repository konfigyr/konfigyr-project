import { setupServer } from 'msw/node';
import account from './server/account';
import oidc from './server/oidc';
import proxy from './server/proxy';

export const handlers = [
  ...account,
  ...oidc,
  ...proxy,
];

export const server = setupServer(...handlers);
