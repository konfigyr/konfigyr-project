import '@testing-library/jest-dom/vitest';

import ResizeObserver from 'resize-observer-polyfill';
import MatchMediaMock from 'vitest-matchmedia-mock';

import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { server } from './helpers/server';
import { authenticationSession } from './helpers/session';

vi.stubGlobal('ResizeObserver', ResizeObserver);

beforeAll(() => server.listen());
afterEach(async () => {
  server.resetHandlers();
  await authenticationSession.clear();
});
afterAll(() => server.close());

if (typeof window !== 'undefined') {
  const matchMedia = new MatchMediaMock();

  afterEach(() => matchMedia.clear());
  afterAll(() => matchMedia.destroy());
}
