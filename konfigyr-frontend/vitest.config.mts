import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';
import viteReact from '@vitejs/plugin-react-swc';
import tsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig({
    plugins: [
        tsconfigPaths(),
        viteReact({
            tsDecorators: true,
            plugins: [
                [
                    '@swc/plugin-formatjs',
                    {
                        idInterpolationPattern: '[md5:contenthash:hex:10]',
                        ast: true,
                    },
                ],
            ],
        }),
    ],
    test: {
        environment: 'jsdom',
        env: loadEnv('test', process.cwd(), ''),
        setupFiles: ['test/vitest-setup.ts'],
        execArgv: ['--no-experimental-webstorage'],
        coverage: {
            provider: 'v8',
            reporter: ['text', 'json'],
            include: ['src/**'],
        },
    },
});
