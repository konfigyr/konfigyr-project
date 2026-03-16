import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { SimplePagination } from '@konfigyr/components/vault/change-history/simple-pagination';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';

describe('components | vault | change-history | <SimplePagination/>', () => {
  afterEach(() => cleanup());

  test('should render <SimplePagination/> component with deactivated buttons', async () => {
    const onClick = vi.fn();

    const { getByRole, getByText, getByLabelText } = renderComponentWithRouter(
      <SimplePagination
        page={0}
        pages={1}
        onClick={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getByLabelText('Go to previous page')).toHaveAttribute('data-active', 'false');
      expect(getByLabelText('Go to next page')).toHaveAttribute('data-active', 'false');
    });

    await userEvents.click(
      getByLabelText('Go to previous page'),
    );

    await userEvents.click(
      getByLabelText('Go to next page'),
    );

    expect(onClick).not.toHaveBeenCalled();
  });


  test('should render <SimplePagination/> component with deactivated Next button', async () => {
    const onClick = vi.fn();

    const { getByLabelText } = renderComponentWithRouter(
      <SimplePagination
        page={1}
        pages={2}
        onClick={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getByLabelText('Go to previous page')).toHaveAttribute('data-active', 'true');
      expect(getByLabelText('Go to next page')).toHaveAttribute('data-active', 'false');
    });

    await userEvents.click(
      getByLabelText('Go to previous page'),
    );

    await userEvents.click(
      getByLabelText('Go to next page'),
    );

    expect(onClick).not.toHaveBeenCalledTimes(1);
  });

  test('should render <SimplePagination/> component with deactivated Previous button', async () => {
    const onClick = vi.fn();

    const { getByLabelText } = renderComponentWithRouter(
      <SimplePagination
        page={0}
        pages={2}
        onClick={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getByLabelText('Go to previous page')).toHaveAttribute('data-active', 'false');
      expect(getByLabelText('Go to next page')).toHaveAttribute('data-active', 'true');
    });

    await userEvents.click(
      getByLabelText('Go to previous page'),
    );

    await userEvents.click(
      getByLabelText('Go to next page'),
    );

    expect(onClick).not.toHaveBeenCalledTimes(1);
  });

  test('should render <SimplePagination/> component with active Next and Previous buttons', async () => {
    const onClick = vi.fn();

    const { getByLabelText } = renderComponentWithRouter(
      <SimplePagination
        page={1}
        pages={3}
        onClick={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getByLabelText('Go to previous page')).toHaveAttribute('data-active', 'true');
      expect(getByLabelText('Go to next page')).toHaveAttribute('data-active', 'true');
    });

    await userEvents.click(
      getByLabelText('Go to previous page'),
    );

    await userEvents.click(
      getByLabelText('Go to next page'),
    );

    expect(onClick).not.toHaveBeenCalledTimes(2);
  });
});
