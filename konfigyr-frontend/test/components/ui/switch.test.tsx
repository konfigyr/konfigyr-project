import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, render } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Switch } from '@konfigyr/components/ui/switch';

describe('components | UI | <Switch/>', () => {
  afterEach(() => cleanup());

  test('should render controlled switch component', async () => {
    const { getByRole } = render(
      <Switch data-testid="inputs" checked={true} />,
    );

    const input = getByRole('switch');
    expect(input).toBeInTheDocument();
    expect(input).toBeChecked();
    expect(input).toHaveAttribute('aria-checked', 'true');

    await userEvents.click(input);

    // this is a controlled component, should not change its state
    expect(input).toBeChecked();
    expect(input).toHaveAttribute('aria-checked', 'true');
  });

  test('should render uncontrolled switch component', async () => {
    const onCheckedChange = vi.fn();

    const { getByRole } = render(
      <Switch data-testid="inputs" onCheckedChange={onCheckedChange} />,
    );

    const input = getByRole('switch');
    expect(input).toBeInTheDocument();
    expect(input).not.toBeChecked();
    expect(input).toHaveAttribute('aria-checked', 'false');

    await userEvents.click(input);

    expect(onCheckedChange).toHaveBeenCalled();

    expect(input).toBeChecked();
    expect(input).toHaveAttribute('aria-checked', 'true');
  });
});
