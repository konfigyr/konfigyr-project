import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { RelativeDate } from '@konfigyr/components/messages/relative-date';

describe('components | messages | <RelativeDate/>', () => {
  afterEach(() => cleanup());

  test('should not render anything when date value is not provided', () => {
    const result = renderWithMessageProvider(
      <RelativeDate value={undefined}/>,
    );

    expect(result.queryByRole('time')).toBeNull();
  });

  test('should render relative date with default title', () => {
    const date = new Date('2026-02-13T08:36:30.141Z');

    const result = renderWithMessageProvider(
      <RelativeDate value={date}/>,
    );

    expect(result.getByRole('time', { name: 'February 13, 2026 at 08:36:30 AM' })).toBeInTheDocument();
  });

  test('should render relative date message with seconds ago', () => {
    const date = new Date(Date.now());

    const result = renderWithMessageProvider(
      <RelativeDate value={date}/>,
    );

    expect(result.getByRole('time')).toHaveTextContent('Just now');
    expect(result.getByRole('time')).toHaveAttribute('datetime', date.toISOString());
  });

  test('should render relative date message for date in minutes and custom title', () => {
    const date = new Date(Date.now() - 1000 * 60 * 8);

    const result = renderWithMessageProvider(
      <RelativeDate title="Accessible title" value={date}/>,
    );

    expect(result.getByRole('time', { name: 'Accessible title'})).toBeInTheDocument();
    expect(result.getByRole('time')).toHaveTextContent('8 minutes ago');
    expect(result.getByRole('time')).toHaveAttribute('datetime', date.toISOString());
  });

  test('should render relative date in future message for date in minutes', () => {
    const date = new Date(Date.now() + 1000 * 60 * 35);

    const result = renderWithMessageProvider(
      <RelativeDate value={date}/>,
    );

    expect(result.getByRole('time')).toHaveTextContent('in 35 minutes');
    expect(result.getByRole('time')).toHaveAttribute('datetime', date.toISOString());
  });

  test('should render relative date message for date in hours', () => {
    const date = new Date(Date.now() - 1000 * 60 * 60 * 3);

    const result = renderWithMessageProvider(
      <RelativeDate value={date}/>,
    );

    expect(result.getByRole('time')).toHaveTextContent('3 hours ago');
    expect(result.getByRole('time')).toHaveAttribute('datetime', date.toISOString());
  });

  test('should render relative date message for date in days', () => {
    const date = new Date(Date.now() - 1000 * 60 * 60 * 24 * 2);

    const result = renderWithMessageProvider(
      <RelativeDate value={date}/>,
    );

    expect(result.getByRole('time')).toHaveTextContent('2 days ago');
    expect(result.getByRole('time')).toHaveAttribute('datetime', date.toISOString());
  });

  test('should render relative date message for date in weeks', () => {
    const date = new Date(Date.now() - 1000 * 60 * 60 * 24 * 33);

    const result = renderWithMessageProvider(
      <RelativeDate value={date}/>,
    );

    expect(result.getByRole('time')).toHaveTextContent('5 weeks ago');
    expect(result.getByRole('time')).toHaveAttribute('datetime', date.toISOString());
  });

  test('should render relative date message for date in months', () => {
    const date = new Date(Date.now() - 1000 * 60 * 60 * 24 * 79);

    const result = renderWithMessageProvider(
      <RelativeDate value={date.toISOString()}/>,
    );

    expect(result.getByRole('time')).toHaveTextContent('3 months ago');
    expect(result.getByRole('time')).toHaveAttribute('datetime', date.toISOString());
  });

  test('should render relative date message for date in years', () => {
    const date = new Date(Date.now() - 1000 * 60 * 60 * 24 * 418);

    const result = renderWithMessageProvider(
      <RelativeDate value={date.getTime()}/>,
    );

    expect(result.getByRole('time')).toHaveTextContent('2 years ago');
    expect(result.getByRole('time')).toHaveAttribute('datetime', date.toISOString());
  });

});
