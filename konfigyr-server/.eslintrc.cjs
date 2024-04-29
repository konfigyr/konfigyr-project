const vitest = require('eslint-plugin-vitest');

module.exports = {
  extends: ['eslint:recommended'],
  plugins: ['prettier'],
  rules: {
    'prettier/prettier': 'error'
  },
  env: {
    es6: true,
    browser: true
  },
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  globals: {
    process: 'readonly'
  },
  overrides: [
    {
      files: ['**/*.test.js'],
      plugins: ['vitest'],
      rules: {
        ...vitest.configs.recommended.rules,
      },
      globals: {
        ...vitest.environments.env.globals,
        fetchMock: 'readonly'
      }
    }
  ]
}