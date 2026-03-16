import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import {
  ChangeHistoryAlert,
} from '@konfigyr/components/vault/change-history/change-history-alert';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';


describe('components | vault | change-history | <ChangeHistoryAlert/>', () => {
  afterEach(() => cleanup());

  test('should render <ChangeHistoryAlert/> component', async () => {

    const { getByText } = renderComponentWithRouter(
      <ChangeHistoryAlert
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        profile={profiles.development}
      />,
    );

    await waitFor(() => {
      expect(getByText('Test User <test.user@ebf.com>', { selector: '.font-bold' })).toBeInTheDocument();
      expect(getByText('Changeset draft')).toBeInTheDocument();
      expect(getByText('9eadce4691d8fcd863aeeb07ef81d8146083d814')).toBeInTheDocument();
      expect(getByText('5 days ago')).toBeInTheDocument();
      expect(getByText('2 Commits')).toBeInTheDocument();
    });
  });
});
