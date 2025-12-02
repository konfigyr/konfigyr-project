import { afterEach, describe, expect, test } from 'vitest';
import { useLocalStorage } from '@konfigyr/hooks/local-storage';
import { act, renderHook } from '@testing-library/react';

const externalValueUpdate = (value: string | null) => {
  if (value === null) {
    localStorage.removeItem('test-key');
  } else {
    localStorage.setItem('test-key', value);
  }

  window.dispatchEvent(new StorageEvent('storage', {
    key: 'test-key', newValue: value,
  }));
};

describe('hooks | local-storage', () => {
  afterEach(() => localStorage.clear());

  test('should return the default value', () => {
    const { result: state } = renderHook(() =>
      useLocalStorage('test-key', 'default-value'),
    );

    expect(state.current[0]).toBe('default-value');
  });

  test('should update state value via dispatch action and reset', () => {
    const { result: state } = renderHook(() =>
      useLocalStorage('test-key', 'default-value'),
    );

    const [_, setValue] = state.current;
    expect(state.current[0]).toBe('default-value');

    act(() => setValue('new-value'));
    expect(state.current[0]).toBe('new-value');

    act(() => setValue(undefined));
    expect(state.current[0]).toBe('default-value');
  });

  test('should compute state value via dispatch action and reset', () => {
    const { result: state } = renderHook(() =>
      useLocalStorage('test-key', 'default-value'),
    );

    const [_, setValue] = state.current;
    expect(state.current[0]).toBe('default-value');

    act(() => setValue(previous => `${previous}-computed`));
    expect(state.current[0]).toBe('default-value-computed');

    act(() => setValue(null));
    expect(state.current[0]).toBe('default-value');
  });

  test('should update the value when local storage event is fired', () => {
    const { result: state } = renderHook(() =>
      useLocalStorage('test-key', 'default-value'),
    );

    act(() => externalValueUpdate('"event-value"'));
    expect(state.current[0]).toBe('event-value');
  });

  test('should fail to process invalid local storage value', () => {
    const { result: state } = renderHook(() =>
      useLocalStorage('test-key', 'default-value'),
    );

    act(() => externalValueUpdate('invalid json string'));
    expect(state.current[0]).toBe('default-value');
  });

  test('should use the default when local storage value is removed', () => {
    const { result: state } = renderHook(() =>
      useLocalStorage('test-key', 'default-value'),
    );

    act(() => externalValueUpdate('"valid-event-value"'));
    expect(state.current[0]).toBe('valid-event-value');

    act(() => externalValueUpdate(null));
    expect(state.current[0]).toBe('default-value');
  });
});
