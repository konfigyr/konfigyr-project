import { afterEach, describe, expect, test } from 'vitest';
import { Goodbye } from '@konfigyr/components/account/goodbye';
import { MessagesProvider } from '@konfigyr/test/helpers/messages';
import { cleanup, render } from '@testing-library/react';

describe('components | account | <Goodbye/>', () => {
  afterEach(() => cleanup());

  test('should render account goodbye message', () => {
    const { getByText } = render((
      <MessagesProvider>
        <Goodbye />
      </MessagesProvider>
    ));

    expect(getByText(
      'Account successfully deleted',
    )).toBeInTheDocument();

    expect(getByText(
      'Your account and its data have been securely removed. ' +
      'We really appreciate the time you spent building, testing, and shipping with us.',
    )).toBeInTheDocument();

    expect(getByText(
      'If you ever feel like starting fresh, you can spin up a new account anytime. ' +
      'Until then, best of luck with your next project.',
    )).toBeInTheDocument();
  });
});
