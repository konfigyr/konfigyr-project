import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { useConfigFileParser } from '@konfigyr/hooks/vault/config-file-parser';

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

describe('hooks | vault | useConfigFileParser', () => {
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
  });

  test('should not fail on scalar JSON/YAML roots', async () => {
    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.parseFile(mockFile('application.json', 'null'));
    });
    expect(result.current.properties).toStrictEqual([]);

    await act(async () => {
      await result.current.parseFile(mockFile('application.yaml', '42'));
    });
    expect(result.current.properties).toStrictEqual([]);
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
  });

  test('should throw and clear properties on parse error', async () => {
    const { result } = renderHook(() => useConfigFileParser());

    await act(async () => {
      await result.current.parseFile(mockFile('application.properties', 'ok=1'));
    });
    expect(result.current.properties).toMatchObject([{ name: 'ok' }]);

    await expect(
      act(async () => {
        await result.current.parseFile(mockFile('application.json', '{invalid'));
      }),
    ).rejects.toBeDefined();

    expect(result.current.properties).toStrictEqual([]);
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

    await waitFor(() => {
      expect(result.current.properties).toMatchObject([{ name: 'current.value' }]);
    });

    await act(async () => {
      slowText.resolve('stale.value=1');
      await slowText.promise;
    });

    await waitFor(() => {
      expect(result.current.properties).toMatchObject([{ name: 'current.value' }]);
      expect(result.current.properties).not.toMatchObject([{ name: 'stale.value' }]);
    });
  });
});
