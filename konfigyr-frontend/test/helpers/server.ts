import { setupServer } from 'msw/node';
import account from './server/account';
import namespaces from './server/namespaces';
import oidc from './server/oidc';
import proxy from './server/proxy';

export const handlers = [
  ...account,
  ...namespaces,
  ...oidc,
  ...proxy,
];

export const server = setupServer(...handlers);
