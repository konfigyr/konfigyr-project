import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render } from '@testing-library/react';
import { CounterStat, StatsCard } from '@konfigyr/components/reporting/stats';

describe('components | reporting | <StatsCard/>', () => {
  afterEach(() => cleanup());

  test('should render stats card', () => {
    const result = render(
      <StatsCard>
        <CounterStat title="Counter" counter={1234} />
      </StatsCard>,
    );

    expect(result.getByText('Counter')).toBeInTheDocument();
    expect(result.getByText('1234')).toBeInTheDocument();
  });

  test('should render an optional cta and footer', () => {
    const result = render(
      <StatsCard>
        <CounterStat
          title="Counter"
          counter={1234}
          cta={<a href="/somewhere">Go</a>}
          footer="Footer text"
        />
      </StatsCard>,
    );

    expect(result.getByRole('link', { name: 'Go' })).toBeInTheDocument();
    expect(result.getByText('Footer text')).toBeInTheDocument();
  });
});
