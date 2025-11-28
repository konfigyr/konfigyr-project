import '@testing-library/jest-dom/vitest';

import { afterAll, afterEach, beforeAll } from 'vitest';
import { server } from './helpers/server';
import { authenticationSession } from './helpers/session';

beforeAll(() => server.listen());
afterEach(async () => {
  server.resetHandlers();
  await authenticationSession.clear();
});
afterAll(() => server.close());
