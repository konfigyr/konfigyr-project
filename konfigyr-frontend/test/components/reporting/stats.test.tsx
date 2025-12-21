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
});
