import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { profiles, services } from '@konfigyr/test/helpers/mocks';
import { ChangeRequestMergeStatus, ChangeRequestState } from '@konfigyr/hooks/vault/types';
import { ChangeRequestStateSummary } from '@konfigyr/components/vault/change-request/change-request-state-summary';

import type { ChangeRequest } from '@konfigyr/hooks/vault/types';

const changeRequest: ChangeRequest = {
  id: 'summary',
  service: services.konfigyrApi,
  profile: profiles.development,
  state: ChangeRequestState.MERGED,
  mergeStatus: ChangeRequestMergeStatus.CHANGES_REQUESTED,
  number: 467,
  subject: 'CR Summary',
  count: 4,
  createdBy: 'John Doe',
  createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
  updatedAt: new Date(Date.now() - 1000 * 60 * 18).toISOString(),
};

describe('components | vault | change-request | <ChangeRequestStateSummary/>', () => {
  afterEach(() => cleanup());

  test('should render <ChangeRequestStateSummary/> component for open state', () => {
    const { container } = renderWithMessageProvider(
      <ChangeRequestStateSummary changeRequest={{ ...changeRequest, state: ChangeRequestState.OPEN }}/>,
    );

    expect(container).toHaveTextContent(
      'John Doe wants to perform 4 changes in development configuration profile.',
    );
  });

  test('should render <ChangeRequestStateSummary/> component for merged state', () => {
    const { container } = renderWithMessageProvider(
      <ChangeRequestStateSummary changeRequest={changeRequest}/>,
    );

    expect(container).toHaveTextContent(
      'John Doe merged 4 changes into development configuration profile 18 minutes ago.',
    );
  });

  test('should render <ChangeRequestStateSummary/> component for discarded state', () => {
    const { container } = renderWithMessageProvider(
      <ChangeRequestStateSummary changeRequest={{
        ...changeRequest,
        count: 1,
        state: ChangeRequestState.DISCARDED,
      }}/>,
    );

    expect(container).toHaveTextContent('John Doe discarded 1 change 18 minutes ago.');
  });

});
