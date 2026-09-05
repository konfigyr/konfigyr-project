import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render } from '@testing-library/react';
import {
  Card,
  CardAction,
  CardContent,
  CardFooter,
  CardHeader,
} from '@konfigyr/components/ui/card';

describe('components | UI | <Card/>', () => {
  afterEach(() => cleanup());

  test('should render card with title and description passed to header props', () => {
    const { getByRole, getByTestId, getByText } = render(
      <Card data-testid="container">
        <CardHeader title="Card title" description="Card description" />
        <CardContent>
          <p>Card contents</p>
        </CardContent>
        <CardFooter>
          <CardAction>
            <a href="#">Card action</a>
          </CardAction>
        </CardFooter>
      </Card>,
    );

    expect(getByTestId('container')).toBeDefined();
    expect(getByText('Card title')).toBeDefined();
    expect(getByText('Card description')).toBeDefined();
    expect(getByRole('paragraph')).toBeDefined();
    expect(getByRole('paragraph')).toHaveTextContent('Card contents');
    expect(getByRole('link')).toBeDefined();
    expect(getByRole('link')).toHaveTextContent('Card action');
  });
});
