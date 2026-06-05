import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { useConfigFileParser } from '@konfigyr/hooks/vault/config-file-parser';

vi.mock('@tanstack/react-start', async () => {
  const { mockTanstackReactStart } = await import('@konfigyr/test/helpers/mocks/tanstack-react-start');
  return mockTanstackReactStart();
});

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const mockFile = (name: string, text: string | Promise<string>): File => {
  return {
    name,
    text: () => typeof text === 'string' ? Promise.resolve(text) : text,
  } as File;
};

const mockJsonFetch = (body: unknown, init: ResponseInit = {}) => {
  vi.spyOn(globalThis, 'fetch').mockImplementation(() => {
    return Promise.resolve(
      new Response(JSON.stringify(body), {
        ...init,
      }),
    );
  });
};

describe('hooks | vault | useConfigFileParser', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  test('should expose ready state by default', () => {
    const { result } = renderHook(() => useConfigFileParser());

    expect(result.current.properties).toStrictEqual([]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);
  });

  test('should parse escaped property keys and odd-backslash continuations', async () => {
    const { result } = renderHook(() => useConfigFileParser());
    const file = mockFile(
      'application.properties',
      [
        'db\\:host=localhost',
        'my\\\\\\ key=escaped-space',
        'message=hello\\',
        'world',
      ].join('\n'),
    );

    await act(async () => {
      await result.current.parseFile(file);
    });

    expect(result.current.properties).toMatchObject([
      { name: 'db:host', value: { encoded: 'localhost', decoded: 'localhost' } },
      { name: 'my\\ key', value: { encoded: 'escaped-space', decoded: 'escaped-space' } },
      { name: 'message', value: { encoded: 'helloworld', decoded: 'helloworld' } },
    ]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);
  });

  test('should not fail on scalar JSON/YAML roots', async () => {
    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.parseFile(mockFile('application.json', 'null'));
    });
    expect(result.current.properties).toStrictEqual([]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);

    await act(async () => {
      await result.current.parseFile(mockFile('application.yaml', '42'));
    });
    expect(result.current.properties).toStrictEqual([]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);
  });

  test('should serialize scalar arrays as comma-separated values for JSON and YAML', async () => {
    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.parseFile(
        mockFile(
          'application.json',
          JSON.stringify({
            tags: ['a', 'b'],
            nums: [1, 2],
            mix: ['x', 2, true],
            items: [{ a: 1 }, { a: 2 }],
          }),
        ),
      );
    });

    expect(result.current.properties).toMatchObject([
      { name: 'tags', value: { encoded: 'a,b', decoded: 'a,b' } },
      { name: 'nums', value: { encoded: '1,2', decoded: '1,2' } },
      { name: 'mix', value: { encoded: 'x,2,true', decoded: 'x,2,true' } },
      { name: 'items[0].a', value: { encoded: '1', decoded: '1' } },
      { name: 'items[1].a', value: { encoded: '2', decoded: '2' } },
    ]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);

    await act(async () => {
      await result.current.parseFile(
        mockFile(
          'application.yaml',
          [
            'tags:',
            '  - a',
            '  - b',
            'nums: [1, 2]',
            'mix: [x, 2, true]',
            'items:',
            '  - a: 1',
            '  - a: 2',
          ].join('\n'),
        ),
      );
    });

    expect(result.current.properties).toMatchObject([
      { name: 'tags', value: { encoded: 'a,b', decoded: 'a,b' } },
      { name: 'nums', value: { encoded: '1,2', decoded: '1,2' } },
      { name: 'mix', value: { encoded: 'x,2,true', decoded: 'x,2,true' } },
      { name: 'items[0].a', value: { encoded: '1', decoded: '1' } },
      { name: 'items[1].a', value: { encoded: '2', decoded: '2' } },
    ]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);
  });

  test('should expose error state on parse error', async () => {
    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.parseFile(mockFile('application.properties', 'ok=1'));
    });
    expect(result.current.properties).toMatchObject([{ name: 'ok' }]);

    await act(async () => {
      await result.current.parseFile(mockFile('application.json', '{invalid'));
    });

    expect(result.current.properties).toStrictEqual([]);
    expect(result.current.error).toBeDefined();
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBeTruthy();
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(true);
    expect(result.current.isReady).toBe(false);
  });

  test('should ignore stale parse results from previous file selection', async () => {
    const { result } = renderHook(() => useConfigFileParser());
    const slowText = deferred<string>();

    const slowFile = mockFile('slow.properties', slowText.promise);
    const fastFile = mockFile('fast.properties', 'current.value=2');

    act(() => {
      void result.current.parseFile(slowFile);
      void result.current.parseFile(fastFile);
    });

    expect(result.current.isParsing).toBe(true);
    expect(result.current.isReady).toBe(false);

    await waitFor(() => {
      expect(result.current.properties).toMatchObject([{ name: 'current.value' }]);
    });

    expect(result.current.error).toBeUndefined();
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);

    await act(async () => {
      slowText.resolve('stale.value=1');
      await slowText.promise;
    });

    await waitFor(() => {
      expect(result.current.properties).toMatchObject([{ name: 'current.value' }]);
      expect(result.current.properties).not.toMatchObject([{ name: 'stale.value' }]);
    });

    expect(result.current.error).toBeUndefined();
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);
  });

  test('should reset data and status', async () => {
    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.parseFile(mockFile('application.properties', 'ok=1'));
    });

    act(() => {
      result.current.reset();
    });

    expect(result.current.properties).toStrictEqual([]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);
  });

  test('should fetch config from config server and merge propertySources in one object', async () => {
    mockJsonFetch({
      propertySources: [
        { name: 'high', source: { 'feature.flag': true } },
        { name: 'low', source: { 'feature.flag': false, 'service.name': 'api' } },
      ],
    });

    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.fetchConfig({
        username: 'user',
        password: 'password',
        configServerUrl: 'https://config.example.test',
      });
    });

    await waitFor(() => {
      expect(result.current.properties).toMatchObject([
        { name: 'feature.flag', value: { encoded: 'true', decoded: 'true' } },
        { name: 'service.name', value: { encoded: 'api', decoded: 'api' } },
      ]);
    });
    expect(result.current.error).toBeUndefined();
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);
  });

  test('should expose error when config server response does not contain propertySources', async () => {
    mockJsonFetch({ name: 'broken-payload' });

    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.fetchConfig({
        username: 'user',
        password: 'pass',
        configServerUrl: 'https://config.example.test',
      });
    });

    await waitFor(() => {
      expect(result.current.properties).toStrictEqual([]);
      expect(result.current.error).toBeDefined();
      expect(result.current.error?.message).toContain('Invalid configuration response: missing propertySources');
    });
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(true);
    expect(result.current.isReady).toBe(false);
  });

  test('should expose error when Config Server request returns 401', async () => {
    mockJsonFetch('Failed to fetch configuration: 401', { status: 401 });

    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.fetchConfig({
        username: 'user',
        password: 'wrong-password',
        configServerUrl: 'https://config.example.test',
      });
    });

    await waitFor(() => {
      expect(result.current.properties).toStrictEqual([]);
      expect(result.current.error).toBeDefined();
      expect(result.current.error?.message).toBe('Failed to fetch configuration: 401');
    });
    expect(result.current.isParsing).toBe(false);
    expect(result.current.isError).toBe(true);
    expect(result.current.isReady).toBe(false);
  });
});
