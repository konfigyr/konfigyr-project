import { HttpResponse, http } from 'msw';
import { encryptingKeyset, signingKeyset } from '../mocks/kms';
import { johnDoe, konfigyr } from '../mocks/namespace';

const create = http.post('http://localhost/api/namespaces/:namespace/kms', async ({ params, request }) => {
  const { namespace } = params;
  const keyset = await request.clone().json() as Record<string, unknown>;

  if (keyset.name === 'existing-keyset') {
    return HttpResponse.json({
      status: 400,
      title: 'Keyset already exists.',
      detail: `Keyset with name '${keyset.name}' already exists in namespace '${namespace}'.`,
    }, { status: 400 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json({ id: 'new-keyset-id', ...keyset });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

const encrypt = http.post('http://localhost/api/namespaces/:namespace/kms/:keyset/encrypt', async ({ params, request }) => {
  const { namespace, keyset } = params;
  const payload = await request.clone().json() as Record<string, unknown>;

  if (keyset !== encryptingKeyset.id) {
    return HttpResponse.json({
      status: 404,
      title: 'Keyset not found.',
      detail: `Keyset with identifier '${keyset}' not found in namespace '${namespace}'.`,
    }, { status: 404 });
  }

  if (payload.plaintext === 'invalid-plaintext') {
    return HttpResponse.json({
      status: 422,
      title: 'Can not perform encryption',
      detail: 'Invalid plaintext.',
    }, { status: 422 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json({ ciphertext: 'encrypted-data' });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

const decrypt = http.post('http://localhost/api/namespaces/:namespace/kms/:keyset/decrypt', async ({ params, request }) => {
  const { namespace, keyset } = params;
  const payload = await request.clone().json() as Record<string, unknown>;

  if (keyset !== encryptingKeyset.id) {
    return HttpResponse.json({
      status: 404,
      title: 'Keyset not found.',
      detail: `Keyset with identifier '${keyset}' not found in namespace '${namespace}'.`,
    }, { status: 404 });
  }

  if (payload.ciphertext === 'invalid-cipher') {
    return HttpResponse.json({
      status: 422,
      title: 'Can not perform decryption',
      detail: 'Invalid ciphertext.',
    }, { status: 422 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json({ plaintext: 'decrypted data' });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

const sign = http.post('http://localhost/api/namespaces/:namespace/kms/:keyset/sign', async ({ params, request }) => {
  const { namespace, keyset } = params;
  const payload = await request.clone().json() as Record<string, unknown>;

  if (keyset !== signingKeyset.id) {
    return HttpResponse.json({
      status: 404,
      title: 'Keyset not found.',
      detail: `Keyset with identifier '${keyset}' not found in namespace '${namespace}'.`,
    }, { status: 404 });
  }

  if (payload.plaintext === 'invalid-plaintext') {
    return HttpResponse.json({
      status: 422,
      title: 'Can not generate signature',
      detail: 'Invalid plaintext.',
    }, { status: 422 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json({ signature: 'signed-data' });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

const verify = http.post('http://localhost/api/namespaces/:namespace/kms/:keyset/verify', async ({ params, request }) => {
  const { namespace, keyset } = params;
  const payload = await request.clone().json() as Record<string, unknown>;

  if (keyset !== signingKeyset.id) {
    return HttpResponse.json({
      status: 404,
      title: 'Keyset not found.',
      detail: `Keyset with identifier '${keyset}' not found in namespace '${namespace}'.`,
    }, { status: 404 });
  }

  if (payload.plaintext === 'invalid-plaintext') {
    return HttpResponse.json({
      status: 422,
      title: 'Can not verify signature',
      detail: 'Invalid plaintext.',
    }, { status: 422 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json({ valid: payload.signature === 'valid-signature' });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

const rotate = http.put('http://localhost/api/namespaces/:namespace/kms/:keyset/rotate', ({ params }) => {
  const { namespace, keyset } = params;

  if (keyset !== signingKeyset.id) {
    return HttpResponse.json({
      status: 404,
      title: 'Keyset not found.',
      detail: `Keyset with identifier '${keyset}' not found in namespace '${namespace}'.`,
    }, { status: 404 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json(signingKeyset);
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

const disable = http.put('http://localhost/api/namespaces/:namespace/kms/:keyset/deactivate', ({ params }) => {
  const { namespace, keyset } = params;

  if (keyset !== signingKeyset.id) {
    return HttpResponse.json({
      status: 404,
      title: 'Keyset not found.',
      detail: `Keyset with identifier '${keyset}' not found in namespace '${namespace}'.`,
    }, { status: 404 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json({ ...signingKeyset, state: 'INACTIVE' });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

const reactivate = http.put('http://localhost/api/namespaces/:namespace/kms/:keyset/reactivate', ({ params }) => {
  const { namespace, keyset } = params;

  if (keyset !== signingKeyset.id) {
    return HttpResponse.json({
      status: 404,
      title: 'Keyset not found.',
      detail: `Keyset with identifier '${keyset}' not found in namespace '${namespace}'.`,
    }, { status: 404 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json({ ...signingKeyset, state: 'ACTIVE' });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

const destroy = http.delete('http://localhost/api/namespaces/:namespace/kms/:keyset', ({ params }) => {
  const { namespace, keyset } = params;

  if (keyset !== signingKeyset.id) {
    return HttpResponse.json({
      status: 404,
      title: 'Keyset not found.',
      detail: `Keyset with identifier '${keyset}' not found in namespace '${namespace}'.`,
    }, { status: 404 });
  }

  if (konfigyr.slug === namespace || johnDoe.slug === namespace) {
    return HttpResponse.json({ ...signingKeyset, state: 'PENDING_DESTRUCTION' });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${namespace}' not found.`,
  }, { status: 404 });
});

export default [
  create,
  encrypt,
  decrypt,
  sign,
  verify,
  rotate,
  reactivate,
  disable,
  destroy,
];
