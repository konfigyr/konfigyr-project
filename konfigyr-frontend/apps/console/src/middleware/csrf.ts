import { createCsrfMiddleware } from '@tanstack/react-start';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export function csrfMiddleware() {
  return createCsrfMiddleware({
    filter: ({ handlerType, request }) => {
      return handlerType === 'serverFn' && !SAFE_METHODS.has(request.method.toUpperCase());
    },
  });
}
