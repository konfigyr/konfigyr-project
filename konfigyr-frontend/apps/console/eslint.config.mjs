import baseConfig from '@konfigyr/config/eslint';
import formatjs from 'eslint-plugin-formatjs';

const eslintConfig = [
  ...baseConfig,
  {
    files: [
      '**/*.+(js|mjs|ts|tsx)',
    ],
    plugins: {
      'formatjs': formatjs,
    },
    rules: {
      'formatjs/enforce-description': ['error', 'literal'],
      'formatjs/enforce-default-message': ['error', 'literal'],
      'formatjs/no-id': ['error'],
      'formatjs/no-useless-message': ['error'],
    },
  },
  {
    ignores: [
      '.nitro/',
      '.output/',
      '.tanstack/',
    ],
  },
];

export default eslintConfig;
