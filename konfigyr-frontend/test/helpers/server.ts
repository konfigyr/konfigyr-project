import { setupServer } from 'msw/node';
import account from './server/account';
import kms from './server/kms';
import namespaces from './server/namespaces';
import oidc from './server/oidc';
import proxy from './server/proxy';
import services from './server/services';

export const handlers = [
  ...account,
  ...kms,
  ...namespaces,
  ...oidc,
  ...proxy,
  ...services,
];

export const server = setupServer(...handlers);
