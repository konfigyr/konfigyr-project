import { setupServer } from 'msw/node';
import account from './server/account';
import changeRequests from './server/change-requests';
import kms from './server/kms';
import namespaces from './server/namespaces';
import applications from './server/namespace/applications';
import invitations from './server/namespace/invitations';
import audit from './server/audit';
import oidc from './server/oidc';
import proxy from './server/proxy';
import services from './server/services';
import vault from './server/vault';

export const handlers = [
  ...account,
  ...changeRequests,
  ...kms,
  ...namespaces,
  ...applications,
  ...invitations,
  ...audit,
  ...oidc,
  ...proxy,
  ...services,
  ...vault,
];

export const server = setupServer(...handlers);
