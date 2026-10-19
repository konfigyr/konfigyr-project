import 'vitest';

import type { TestingLibraryMatchers } from '@testing-library/jest-dom/matchers';

/**
 * Registers the `@testing-library/jest-dom` matchers on the Vitest `expect` types.
 *
 * Vitest reads custom matcher types from `Matchers<R, T>`. The typings shipped with
 * `@testing-library/jest-dom/vitest` augment the single-parameter `Assertion<T>`,
 * which no longer merges with the Vitest assertion type.
 */
declare module 'vitest' {
  // type parameters must match the Vitest declaration to be merged
  // eslint-disable-next-line @typescript-eslint/naming-convention
  interface Matchers<R extends void | Promise<void> = void | Promise<void>, T = unknown>
    extends TestingLibraryMatchers<unknown, R> {}
}
