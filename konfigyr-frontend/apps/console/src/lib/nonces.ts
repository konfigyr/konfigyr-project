import { createIsomorphicFn } from '@tanstack/react-start';
import { getRequest } from '@tanstack/react-start/server';

const store = new WeakMap<Request, string>();

export function generateNonce(): string {
  return Buffer.from(crypto.getRandomValues(new Uint8Array(16))).toString('base64');
}

export const getNonce = createIsomorphicFn()
  .server(() => store.get(getRequest()))
  .client(() => undefined);

export const setNonce = createIsomorphicFn()
  .server((nonce: string) => {
    store.set(getRequest(), nonce);
  })
  .client(() => undefined);
