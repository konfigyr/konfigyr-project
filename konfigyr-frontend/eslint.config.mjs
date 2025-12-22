import stylistic from '@stylistic/eslint-plugin';
import { tanstackConfig } from '@tanstack/eslint-config';
import formatjs from 'eslint-plugin-formatjs';

const eslintConfig = [
  ...tanstackConfig,
  {
    files: [
      '**/*.+(js|mjs|ts|tsx)',
    ],
    plugins: {
      '@stylistic': stylistic,
      'formatjs': formatjs,
    },
    rules: {
      'formatjs/enforce-description': ['error', 'literal'],
      'formatjs/enforce-default-message': ['error', 'literal'],
      'formatjs/no-id': ['error'],
      'formatjs/no-useless-message': ['error'],
      '@stylistic/comma-dangle': ['error', 'always-multiline'],
      '@stylistic/indent': ['error', 2],
      '@stylistic/no-trailing-spaces': ['error'],
      '@stylistic/quotes': ['error', 'single'],
      '@stylistic/semi': ['error', 'always'],
      '@stylistic/space-before-blocks': ['error', 'always'],
    },
  },
  {
    ignores: [
      'node_modules/',
      '.nitro/',
      '.output/',
      '.tanstack/',
      '**/*.gen.ts',
    ],
  },
];

export default eslintConfig;
