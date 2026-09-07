import { defineConfig } from 'vitest/config';
import viteReact from '@vitejs/plugin-react-swc';

import type { UserConfig } from 'vite';

export type VitestConfig = UserConfig & {
  react?: Parameters<typeof viteReact>[0];
};

/**
 * Shared vitest config for workspace packages.
 *
 * Provides sensible defaults (jsdom environment, tsconfig path resolution,
 * v8 coverage over src/**) while letting the caller override or extend
 * any part of it, e.g. to add its own Vite plugins.
 */
export function createVitestConfig({ resolve = {}, plugins = [], react, test = {}, ...config }: VitestConfig = {}): UserConfig {
  return defineConfig({
    resolve: {
      tsconfigPaths: true,
      ...resolve,
    },
    plugins: [
      viteReact(react),
      ...plugins,
    ],
    test: {
      environment: 'jsdom',
      setupFiles: ['@konfigyr/vitest-config/setup'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json'],
        include: ['src/**'],
      },
      ...test,
    },
    ...config,
  });
}
