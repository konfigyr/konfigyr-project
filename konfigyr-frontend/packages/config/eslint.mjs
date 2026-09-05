import stylistic from '@stylistic/eslint-plugin';
import { tanstackConfig } from '@tanstack/eslint-config';

const baseConfig = [
  ...tanstackConfig,
  {
    files: [
      '**/*.+(js|mjs|ts|tsx)',
    ],
    plugins: {
      '@stylistic': stylistic,
    },
    rules: {
      '@stylistic/block-spacing': ['error'],
      '@stylistic/comma-dangle': ['error', 'always-multiline'],
      '@stylistic/implicit-arrow-linebreak': ['error', 'beside'],
      '@stylistic/indent': ['error', 2],
      '@stylistic/no-multi-spaces': ['error'],
      '@stylistic/no-trailing-spaces': ['error'],
      '@stylistic/quotes': ['error', 'single'],
      '@stylistic/jsx-closing-tag-location': ['error'],
      '@stylistic/jsx-curly-spacing': ['error', { when: 'never' }],
      '@stylistic/jsx-equals-spacing': ['error', 'never'],
      '@stylistic/object-curly-newline': ['error', { consistent: true }],
      '@stylistic/object-curly-spacing': ['error', 'always'],
      '@stylistic/semi': ['error', 'always'],
      '@stylistic/space-before-blocks': ['error', 'always'],
      '@stylistic/switch-colon-spacing': ['error'],
      '@stylistic/rest-spread-spacing': ['error', 'never'],
      '@stylistic/type-annotation-spacing': ['error'],
      '@stylistic/type-generic-spacing': ['error'],
    },
  },
  {
    ignores: [
      'node_modules/',
      '**/*.gen.ts',
    ],
  },
];

export default baseConfig;
