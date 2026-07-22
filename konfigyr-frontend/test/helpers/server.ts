import { setupServer } from 'msw/node';
import account from './server/account';
import changeRequests from './server/change-requests';
import kms from './server/kms';
import memberships from './server/memberships';
import namespaces from './server/namespaces';
import applications from './server/namespace/applications';
import groups from './server/namespace/groups';
import invitations from './server/namespace/invitations';
import transfers from './server/namespace/transfers';
import audit from './server/audit';
import oidc from './server/oidc';
import proxy from './server/proxy';
import services from './server/services';
import vault from './server/vault';

export const handlers = [
  ...account,
  ...changeRequests,
  ...kms,
  ...memberships,
  ...namespaces,
  ...applications,
  ...groups,
  ...invitations,
  ...transfers,
  ...audit,
  ...oidc,
  ...proxy,
  ...services,
  ...vault,
];

export const server = setupServer(...handlers);
