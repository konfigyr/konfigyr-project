import { HttpResponse, http } from 'msw';

const assertBearerToken = (request: Request) => {
  const authorizationHeader = request.headers.get('authorization');

  if (authorizationHeader !== 'Bearer access-token') {
    throw new Error('Authorization header is missing or invalid.');
  }
};

export default [
  http.all('https://api.konfigyr.com/proxy-test/network-error', () => HttpResponse.error()),

  http.get('https://api.konfigyr.com/proxy-test/server-error', ({ request }) => {
    assertBearerToken(request);

    return new HttpResponse(JSON.stringify({
      status: 500,
      title: 'Internal server error',
      detail: 'Unexpected error occurred.',
    }), { status: 500 });
  }),

  http.post('https://api.konfigyr.com/proxy-test/create-resource', async ({ request }) => {
    assertBearerToken(request);

    const body = await request.json() as Record<string, unknown>;

    if (body.name !== 'test-resource') {
      return new HttpResponse(JSON.stringify({
        status: 400,
        title: 'Bad request',
        detail: 'Missing resource name in the request body.',
      }), { status: 400 });
    }

    return new HttpResponse(JSON.stringify({
      id: 'test-resource-id',
      ...body,
    }), { status: 200 });
  }),

  http.patch('https://api.konfigyr.com/proxy-test/unavailable', async ({ request }) => {
    assertBearerToken(request);

    const data = await request.formData();
    const status = parseInt(data.get('status') as string);

    if (isNaN(status)) {
      return new HttpResponse(JSON.stringify({
        status: 400,
        title: 'Bad request',
        detail: 'Missing status code in the request body.',
      }), { status: 400 });
    }

    return new HttpResponse(JSON.stringify({
      status,
      title: 'Bad request',
      detail: 'Missing resource name in the request body.',
    }), { status });
  }),

  http.delete('https://api.konfigyr.com/proxy-test/verify-headers', ({ request }) => {
    assertBearerToken(request);

    if (request.headers.get('x-request-id') !== 'request-id') {
      return new HttpResponse('The x-request-id header is not proxied.', { status: 400 });
    }

    if (request.headers.get('content-type') !== 'application/json') {
      return new HttpResponse('The content-type header is not proxied.', { status: 400 });
    }

    if (request.headers.get('host') === 'original.host') {
      return new HttpResponse('Host header is not sanitized.', { status: 400 });
    }

    if (request.headers.get('cookies') === 'original.cookie') {
      return new HttpResponse('Cookie header is not sanitized.', { status: 400 });
    }

    return new HttpResponse('', { status: 204 });
  }),
];
