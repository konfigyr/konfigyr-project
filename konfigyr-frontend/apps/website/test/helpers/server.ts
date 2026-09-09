import { setupServer } from 'msw/node';
import hubspot from './server/hubspot';

export const handlers = [
  ...hubspot,
];

export const server = setupServer(...handlers);
