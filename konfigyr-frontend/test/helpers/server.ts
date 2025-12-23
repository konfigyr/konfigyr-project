import { setupServer } from 'msw/node';
import account from './server/account';
import namespaces from './server/namespaces';
import oidc from './server/oidc';
import proxy from './server/proxy';
import services from './server/services';

export const handlers = [
  ...account,
  ...namespaces,
  ...oidc,
  ...proxy,
  ...services,
];

export const server = setupServer(...handlers);
