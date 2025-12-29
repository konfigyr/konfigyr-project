import { afterAll, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { SearchToggle } from '@konfigyr/components/layout/search-toggle';

describe('components | layout | <SearchToggle/>', () => {
  const result = renderWithMessageProvider(
    <SearchToggle />,
  );

  afterAll(() => cleanup());

  test('should render search toggle button that would open the search dialog', async () => {
    expect(result.getByRole('button', { name: 'Search...'})).toBeInTheDocument();

    await userEvents.click(
      result.getByRole('button'),
    );

    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Search');
    expect(result.getByRole('heading')).toHaveAccessibleName('Search');
  });

  test('search dialog show contain the search form', () => {
    expect(result.getByRole('searchbox')).toBeInTheDocument();
  });

  test('search dialog show contain the close key', () => {
    expect(result.getByRole('button', { name: 'Close search' })).toBeInTheDocument();
  });

  test('should close the dialog by pressing ESC key', async () => {
    await userEvents.keyboard('{Escape}');

    await waitFor(() => {
      expect(result.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  test('should open and close dialog by pressing shortcut', async () => {
    await userEvents.keyboard('{meta>}k{/meta}');

    await waitFor(() => {
      expect(result.queryByRole('dialog')).toBeInTheDocument();
    });

    await userEvents.keyboard('{meta>}k{/meta}');

    await waitFor(() => {
      expect(result.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });
});
