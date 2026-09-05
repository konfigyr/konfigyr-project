import { afterEach, describe, expect, test } from 'vitest';
import { DotIcon } from 'lucide-react';
import { cleanup, render } from '@testing-library/react';
import { SimpleAlert } from '@konfigyr/components/ui/alert';

describe('components | UI | <SimpleAlert/>', () => {
  afterEach(() => cleanup());

  test('should render simple alert with just the title', () => {
    const { queryByRole } = render(
      <SimpleAlert title="Alert title" />,
    );

    const alert = queryByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveAccessibleName('Alert title');
    expect(alert).not.toHaveAccessibleDescription();
  });

  test('should render simple alert with just the icon', () => {
    const { queryByRole, queryByTestId } = render(
      <SimpleAlert icon={<DotIcon data-testid="icon" />} />,
    );

    const alert = queryByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).not.toHaveAccessibleName();
    expect(alert).not.toHaveAccessibleDescription();

    // make sure that the ARIA attributes are not set
    expect(alert).not.toHaveAttribute('aria-labelledby');
    expect(alert).not.toHaveAttribute('aria-describedby');

    expect(queryByTestId('icon')).toBeInTheDocument();
  });

  test('should render simple alert with title and description', () => {
    const { queryByRole } = render(
      <SimpleAlert title="Alert title" description="Alert description" />,
    );

    const alert = queryByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveAccessibleName('Alert title');
    expect(alert).toHaveAccessibleDescription('Alert description');

    // make sure that the ARIA attributes are set
    expect(alert).toHaveAttribute('aria-labelledby');
    expect(alert).toHaveAttribute('aria-describedby');
  });

  test('should render simple alert with title, description, icon, action and children', () => {
    const { queryByRole, queryByTestId } = render(
      <SimpleAlert
        title="Alert title"
        description="Alert description"
        icon={<DotIcon data-testid="icon" />}
        action={<p data-testid="action">Action section</p>}
      >
        <p data-testid="contents">Alert contents</p>
      </SimpleAlert>,
    );

    const alert = queryByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveAccessibleName('Alert title');
    expect(alert).toHaveAccessibleDescription('Alert description');

    expect(queryByTestId('icon')).toBeInTheDocument();

    expect(queryByTestId('action')).toBeInTheDocument();
    expect(queryByTestId('action')).toHaveTextContent('Action section');

    expect(queryByTestId('contents')).toBeInTheDocument();
    expect(queryByTestId('contents')).toHaveTextContent('Alert contents');
  });
});
