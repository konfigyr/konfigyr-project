import { describe, expect, test } from 'vitest';
import { parseConfigFile } from '@konfigyr/hooks/vault/config-file-parser';

const mockFile = (name: string, text: string): File => {
  return {
    name,
    text: () => Promise.resolve(text),
  } as File;
};

describe('hooks | vault | parseConfigFile', () => {
  test('should parse escaped property keys and odd-backslash continuations', async () => {
    const file = mockFile(
      'application.properties',
      [
        'db\\:host=localhost',
        'my\\\\\\ key=escaped-space',
        'message=hello\\',
        'world',
      ].join('\n'),
    );

    await expect(parseConfigFile(file)).resolves.toMatchObject([
      { name: 'db:host', value: { encoded: 'localhost', decoded: 'localhost' } },
      { name: 'my\\ key', value: { encoded: 'escaped-space', decoded: 'escaped-space' } },
      { name: 'message', value: { encoded: 'helloworld', decoded: 'helloworld' } },
    ]);
  });

  test('should not fail on scalar JSON/YAML roots', async () => {
    await expect(parseConfigFile(mockFile('application.json', 'null'))).resolves.toStrictEqual([]);
    await expect(parseConfigFile(mockFile('application.yaml', '42'))).resolves.toStrictEqual([]);
  });

  test('should serialize scalar arrays as comma-separated values for JSON and YAML', async () => {
    const expected = [
      { name: 'tags', value: { encoded: 'a,b', decoded: 'a,b' } },
      { name: 'nums', value: { encoded: '1,2', decoded: '1,2' } },
      { name: 'mix', value: { encoded: 'x,2,true', decoded: 'x,2,true' } },
      { name: 'items[0].a', value: { encoded: '1', decoded: '1' } },
      { name: 'items[1].a', value: { encoded: '2', decoded: '2' } },
    ];

    await expect(parseConfigFile(
      mockFile(
        'application.json',
        JSON.stringify({
          tags: ['a', 'b'],
          nums: [1, 2],
          mix: ['x', 2, true],
          items: [{ a: 1 }, { a: 2 }],
        }),
      ),
    )).resolves.toMatchObject(expected);

    await expect(parseConfigFile(
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
    )).resolves.toMatchObject(expected);
  });

  test('should reject when the file content is invalid', async () => {
    await expect(parseConfigFile(mockFile('application.json', '{invalid'))).rejects.toBeInstanceOf(Error);
  });

  test('should reject unsupported file types', async () => {
    await expect(parseConfigFile(mockFile('application.txt', 'ok=1'))).rejects.toThrow('Unsupported file type');
  });
});
