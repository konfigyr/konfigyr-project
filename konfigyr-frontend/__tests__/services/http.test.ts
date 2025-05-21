import { afterEach, describe, expect, vi, test, Mock } from 'vitest';
import request, { createErrorResponse, HttpResponseError, PROBLEM_DETAIL_CONTENT_TYPE } from 'konfigyr/services/http';

const createMockedResponse = (status: number, body: unknown) => {
  global.fetch = vi.fn(() => Promise.resolve({
    status,
    json: () => Promise.resolve(body),
  })) as Mock;
};

describe('services | http', () => {

  afterEach(() => {
    vi.clearAllMocks();
  });

  test('should create Next problem details response using defaults', async () => {
    const response = createErrorResponse({});

    expect(response.status).toStrictEqual(500);
    expect(response.headers.get('content-type')).toStrictEqual(PROBLEM_DETAIL_CONTENT_TYPE);

    expect(await response.json()).toStrictEqual({
      status: 500,
      type: 'about:blank',
    });
  });

  test('should create Next problem details response', async () => {
    const problem = {
      status: 400,
      type: 'some type',
      title: 'Error title',
      detail: 'Error detail',
    };

    const response = createErrorResponse(problem);

    expect(response.status).toStrictEqual(problem.status);
    expect(response.headers.get('content-type')).toStrictEqual(PROBLEM_DETAIL_CONTENT_TYPE);

    expect(await response.json()).toStrictEqual(problem);
  });

  test('should execute successful request', async () => {
    createMockedResponse(200, { data: 'testing' });

    const response = await request('https://example.com');

    expect(response).toStrictEqual({ data: 'testing' });
  });

  test('should reject request with HTTP response error', async () => {
    createMockedResponse(400, { type: 'bad-request', title: 'Bad request', status: 400 });

    const promise = request('https://example.com');

    await expect(promise).rejects.toThrowError(HttpResponseError);
    await expect(promise).rejects.toThrowError(expect.objectContaining({
      status: 400,
      problem: { type: 'bad-request', title: 'Bad request', status: 400 },
    }));
  });

});
