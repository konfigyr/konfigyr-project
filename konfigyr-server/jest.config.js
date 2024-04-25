export default {
  verbose: true,
  collectCoverage: true,
  coverageReporters: ['text', 'cobertura'],
  reporters: [
      'github-actions', 'summary', 'default'
  ],
  setupFiles: [
      './src/main/assets/test/setup.js'
  ],
  testEnvironment: 'jsdom',
  transformIgnorePatterns: ['\\.pnp\\.[^\\\/]+$'],
}