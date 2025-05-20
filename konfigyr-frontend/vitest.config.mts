import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import tsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig({
    plugins: [tsconfigPaths(), react()],
    test: {
        environment: 'jsdom',
        env: loadEnv('test', process.cwd(), ''),
        setupFiles: ['__tests__/vitest-setup.js'],
        coverage: {
            provider: 'v8',
            reporter: ['text', 'json'],
            include: ['src/**'],
        },
    },
});
