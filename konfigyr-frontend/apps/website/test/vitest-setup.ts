import '@konfigyr/vitest-config/setup';

import ResizeObserver from 'resize-observer-polyfill';

import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { server } from './helpers/server';

vi.stubGlobal('ResizeObserver', ResizeObserver);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

if (typeof window !== 'undefined') {
  const IntersectionObserverMock = vi.fn(class {
    disconnect = vi.fn();
    observe = vi.fn();
    takeRecords = vi.fn();
    unobserve = vi.fn();
  });

  vi.stubGlobal('IntersectionObserver', IntersectionObserverMock);

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

  window.scrollTo = vi.fn();
  window.HTMLElement.prototype.scrollIntoView = vi.fn();
  window.HTMLElement.prototype.releasePointerCapture = vi.fn();
  window.HTMLElement.prototype.hasPointerCapture = vi.fn();
  window.HTMLElement.prototype.getAnimations = vi.fn(() => []);
}
