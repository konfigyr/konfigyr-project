import { setupServer } from 'msw/node';
import account from './server/account';
import kms from './server/kms';
import namespaces from './server/namespaces';
import applications from './server/namespace/applications';
import oidc from './server/oidc';
import proxy from './server/proxy';
import services from './server/services';
import vault from './server/vault';

export const handlers = [
  ...account,
  ...kms,
  ...namespaces,
  ...applications,
  ...oidc,
  ...proxy,
  ...services,
  ...vault,
];

export const server = setupServer(...handlers);
