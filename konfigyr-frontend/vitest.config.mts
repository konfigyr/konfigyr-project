import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';
import svgr from 'vite-plugin-svgr';
import viteReact from '@vitejs/plugin-react-swc';

export default defineConfig({
    resolve: {
        tsconfigPaths: true,
    },
    plugins: [
        viteReact({
            tsDecorators: true,
            plugins: [
                [
                    '@swc/plugin-formatjs',
                    {
                        idInterpolationPattern: '[sha512:contenthash:base64:10]',
                        ast: true,
                    },
                ],
            ],
        }),
        svgr({
            include: '**/*.svg',
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
