import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';
import { ChangeHistoryTimeline } from '@konfigyr/components/vault/change-history/change-history';


describe('components | vault | change-history | <ChangeHistoryTimeline/>', () => {
  afterEach(() => cleanup());

  test('should render <ChangeHistoryTimeline/> component', async () => {
    const { getByText, getByLabelText } = renderComponentWithRouter(
      <ChangeHistoryTimeline
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        profile={profiles.development}
      />,
    );

    await waitFor(() => {
      expect(getByText('Test User <test.user@ebf.com>')).toBeInTheDocument();
      expect(getByText('Changeset draft')).toBeInTheDocument();
      expect(getByText('9eadce4691d8fcd863aeeb07ef81d8146083d814')).toBeInTheDocument();
      expect(getByText('5 days ago')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(getByLabelText('Go to previous page')).toHaveAttribute('data-active', 'false');
      expect(getByLabelText('Go to next page')).toHaveAttribute('data-active', 'true');
    });
  });
});
