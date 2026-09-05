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

`npm test` runs typecheck, lint, and the test suite with coverage — this is what Turborepo runs
(with `typescript` and `lint` sequenced first) and what CI invokes via Gradle:

```bash
npm test
```

For interactive, watch-mode development, use the Vitest UI instead:

```bash
npm run test:ui
```

The tests are executed via [vitest](https://vitest.dev) with support of the following plugins:
* [@testing-library/react](https://testing-library.com/docs/react-testing-library/intro/)
* [@testing-library/jest-dom](https://testing-library.com/docs/ecosystem-jest-dom)
* [msw](https://www.npmjs.com/package/msw)

### Linting

This project uses ESLint for linting. To run the linter on its own, run the following command:
```bash
npm run lint
```

Coverage is collected using the [v8 coverage provider](https://vitest.dev/guide/coverage.html#v8-provider).

## Deployment

The application is bundled with a Gradle Docker task. To build the Docker image, run the following command:
```bash
./gradlew konfigyr-frontend:dockerBuild
```

Visit the TanStack Start [hosting guide](https://tanstack.com/start/latest/docs/framework/react/guide/hosting#nodejs--railway--docker)
for more information.
