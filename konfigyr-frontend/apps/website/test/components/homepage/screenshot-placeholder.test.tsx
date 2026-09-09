import { describe, expect, test } from 'vitest';
import { defineMessage } from 'react-intl';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ScreenshotPlaceholder } from '@konfigyr/components/homepage/screenshot-placeholder';

const description = defineMessage({
  defaultMessage: 'A screenshot of the thing that needs a screenshot.',
  description: 'Test fixture caption for the screenshot placeholder',
});

describe('components | homepage | screenshot-placeholder', () => {
  test('renders the unresolved badge and the given caption', () => {
    const { getByText } = renderWithMessageProvider(
      <ScreenshotPlaceholder description={description} />,
    );

    expect(getByText('Screenshot placeholder: not yet added')).toBeInTheDocument();
    expect(getByText('A screenshot of the thing that needs a screenshot.')).toBeInTheDocument();
  });

  test('reserves a wide, fixed aspect-ratio box so real images cause no layout shift', () => {
    const { container } = renderWithMessageProvider(
      <ScreenshotPlaceholder description={description} />,
    );

    const figure = container.querySelector('[data-slot="screenshot-placeholder"]');

    expect(figure).toBeInTheDocument();
    expect(figure).toHaveClass('aspect-video');
  });

  test('is visually flagged as unresolved, consistent with the warning styling used elsewhere', () => {
    const { container } = renderWithMessageProvider(
      <ScreenshotPlaceholder description={description} />,
    );

    const figure = container.querySelector('[data-slot="screenshot-placeholder"]');

    expect(figure).toHaveClass('border-dashed', 'border-warning/60', 'bg-warning/10');
  });
});
