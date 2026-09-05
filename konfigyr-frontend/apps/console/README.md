# Konfigyr UI application

Konfigyr UI is a [TanStack Start](https://tanstack.com/start) application. For more information about TanStack,
visit the [tanstack.com/start](https://tanstack.com/start).

## Getting Started

First, run the development server:

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

## Testing

To run the tests, run the following command:

```bash
npm test
```

The tests are executed via [vitest](https://vitest.dev) with support of the following plugins:
* [@testing-library/react](https://testing-library.com/docs/react-testing-library/intro/)
* [@testing-library/jest-dom](https://testing-library.com/docs/ecosystem-jest-dom)
* [msw](https://www.npmjs.com/package/msw)

### Linting

This project uses ESLint for linting. To run the linter, run the following command:
```bash
npm run lint
`````

### Coverage

Coverage is collected using [v8 coverage provider](https://vitest.dev/guide/coverage.html#v8-provider).
To run tests with coverage, run the following command:
```bash
npm run test:coverage
```

## Deployment

The application is bundled with a Gradle Docker task. To build the Docker image, run the following command:
```bash
./gradlew konfigyr-frontend:dockerBuild
```

Visit the TanStack Start [hosting guide](https://tanstack.com/start/latest/docs/framework/react/guide/hosting#nodejs--railway--docker)
for more information.
