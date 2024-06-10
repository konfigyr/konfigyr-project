import { defineConfig } from 'vitest/config'

const CI = true;

export default defineConfig({
    test: {
        globals: true,
        watch: !CI,
        environment: 'jsdom',
        include: ['./src/main/assets/test/**/*.{test,spec}.js'],
        setupFiles: ['./src/main/assets/test/setup.js'],
        coverage: {
            enabled: CI,
            provider: 'v8',
            reporter: ['text', 'json'],
            include: ['src/main/assets/**'],
        },
    },
})