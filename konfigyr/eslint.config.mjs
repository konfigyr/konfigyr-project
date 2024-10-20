import globals from 'globals';
import prettier from 'eslint-plugin-prettier';
import vitest from 'eslint-plugin-vitest';
import js from '@eslint/js';

export default [
    {
        plugins: { prettier },

        languageOptions: {
            globals: {
                ...globals.browser,
                process: 'readonly',
            },

            ecmaVersion: 'latest',
            sourceType: 'module',
        },

        rules: {
            ...js.configs.recommended.rules,
            ...prettier.configs.recommended.rules,
        },
    },
    {
        files: ['**/*.test.js'],

        plugins: { vitest },

        languageOptions: {
            globals: {
                suite: true,
                test: true,
                describe: true,
                it: true,
                expect: true,
                assert: true,
                vitest: true,
                vi: true,
                beforeAll: true,
                afterAll: true,
                beforeEach: true,
                afterEach: true,
                fetchMock: 'readonly',
            },
        },

        rules: {
            ...vitest.configs.recommended.rules,
            'vitest/expect-expect': 'error',
            'vitest/no-identical-title': 'error',
            'vitest/no-commented-out-tests': 'error',
            'vitest/valid-title': 'error',
            'vitest/valid-expect': 'error',
            'vitest/valid-describe-callback': 'error',
            'vitest/require-local-test-context-for-concurrent-snapshots': 'error',
            'vitest/no-import-node-test': 'error',
        },
    },
];
