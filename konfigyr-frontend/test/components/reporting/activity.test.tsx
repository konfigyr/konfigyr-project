import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render } from '@testing-library/react';
import {
  ActivityCard,
  ActivityCardContent,
  ActivityCardEmpty,
  ActivityCardTitle,
} from '@konfigyr/components/reporting/activity';

describe('components | reporting | <ActivityCard/>', () => {
  afterEach(() => cleanup());

  test('should render activity card with content', () => {
    const result = render(
      <ActivityCard>
        <ActivityCardTitle>Test activity</ActivityCardTitle>
        <ActivityCardContent>Activity content</ActivityCardContent>
      </ActivityCard>,
    );

    expect(result.getByText('Test activity')).toBeInTheDocument();
    expect(result.getByText('Activity content')).toBeInTheDocument();
  });

  test('should render activity card with an empty state', () => {
    const result = render(
      <ActivityCard>
        <ActivityCardTitle>Test activity</ActivityCardTitle>
        <ActivityCardEmpty title="No activity" />
      </ActivityCard>,
    );

    expect(result.getByText('Test activity')).toBeInTheDocument();
    expect(result.getByText('No activity')).toBeInTheDocument();
  });
});
