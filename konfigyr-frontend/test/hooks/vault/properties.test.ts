import { describe, expect, test } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { usePropertyDescriptorSearch } from '@konfigyr/hooks/vault/properties';

import type { PropertyDescriptor } from '@konfigyr/hooks/artifactory/types';

const descriptors: Array<PropertyDescriptor> = [{
  name: 'logging.config',
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  description: 'Location of the logging configuration file. For instance, `classpath:logback.xml` for Logback.',
}, {
  name: 'logging.file.max-history',
  typeName: 'java.lang.Integer',
  schema: { type: 'integer' },
  description: 'Maximum number of archive log files to keep. Only supported with the default logback setup.',
}, {
  name: 'logging.file.max-size',
  typeName: 'org.springframework.util.unit.DataSize',
  schema: { type: 'string', format: 'data-size' },
  description: 'Maximum log file size. Only supported with the default logback setup.',
}, {
  name: 'spring.config.location',
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  description: 'Config file locations that replace the defaults.',
}, {
  name: 'spring.config.name',
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  description: 'Config name.',
}];

describe('hooks | vault | properties', () => {

  test('should perform property descriptor search by term and sort by rank', () => {
    const { result: search } = renderHook(
      (props: { term: string }) => usePropertyDescriptorSearch(descriptors, props.term, 0),
      { initialProps: { term: 'logging file size' } },
    );

    expect(search.current).toHaveLength(4);

    expect(search.current.map(it => it.name)).toStrictEqual([
      'logging.file.max-size',
      'logging.file.max-history',
      'logging.config',
      'spring.config.location',
    ]);
  });

  test('should perform property descriptor search by matching the name prefix', () => {
    const { result: search } = renderHook(
      (props: { term: string }) => usePropertyDescriptorSearch(descriptors, props.term),
      { initialProps: { term: 'logging.file' } },
    );

    expect(search.current).toHaveLength(2);

    expect(search.current.map(it => it.name)).toStrictEqual([
      'logging.file.max-history',
      'logging.file.max-size',
    ]);
  });

  test('should perform property descriptor search by matching the exact name', () => {
    const { result: search } = renderHook(
      (props: { term: string }) => usePropertyDescriptorSearch(descriptors, props.term),
      { initialProps: { term: 'spring.config.name' } },
    );

    expect(search.current).toHaveLength(1);
    expect(search.current[0].name).toStrictEqual('spring.config.name');
    expect(search.current[0].score).toStrictEqual(100);
  });

  test('should debounce property descriptor search term', async () => {
    const { result: search, rerender } = renderHook(
      (props: { term: string }) => usePropertyDescriptorSearch(descriptors, props.term, 400),
      { initialProps: { term: '' } },
    );

    expect(search.current).toStrictEqual(descriptors);

    rerender({ term: 'logging' });
    expect(search.current).toHaveLength(descriptors.length);

    rerender({ term: 'logging.file.max-size' });
    expect(search.current).toHaveLength(descriptors.length);

    await waitFor(() => expect(search.current).toHaveLength(1));
  });
});
