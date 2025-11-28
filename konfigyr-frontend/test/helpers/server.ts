import { setupServer } from 'msw/node';
import oidc from './server/oidc';
import proxy from './server/proxy';

export const handlers = [
  ...oidc,
  ...proxy,
];

export const server = setupServer(...handlers);
