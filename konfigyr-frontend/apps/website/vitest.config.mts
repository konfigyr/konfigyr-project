import { loadEnv } from 'vite';
import { createVitestConfig } from '@konfigyr/vitest-config';

export default createVitestConfig({
    react: {
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
    },
    test: {
        env: loadEnv('test', process.cwd(), ''),
        setupFiles: ['test/vitest-setup.ts'],
        execArgv: ['--no-experimental-webstorage'],
    },
});
