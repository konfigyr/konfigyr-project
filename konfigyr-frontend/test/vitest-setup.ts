import '@testing-library/jest-dom/vitest';

import ResizeObserver from 'resize-observer-polyfill';

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
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(), // deprecated
      removeListener: vi.fn(), // deprecated
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });

  Object.defineProperty(window, 'clipboardData', {
    value: {
      setData: vi.fn(),
      clearData: vi.fn(),
    },
    configurable: true,
  });

  window.HTMLElement.prototype.scrollIntoView = vi.fn();
  window.HTMLElement.prototype.releasePointerCapture = vi.fn();
  window.HTMLElement.prototype.hasPointerCapture = vi.fn();
}

declare global {
  interface Window {
    clipboardData: {
      setData: (format: string, data: string) => void,
      clearData: () => void,
    },
  }
}
