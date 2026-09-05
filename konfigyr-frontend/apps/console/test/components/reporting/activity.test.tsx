import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor, within } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { server } from '@konfigyr/test/helpers/server';
import { audit, namespaces } from '@konfigyr/test/helpers/mocks';
import { ActivityCard } from '@konfigyr/components/reporting/activity';

describe('components | reporting | <ActivityCard/>', () => {
  afterEach(() => cleanup());

  test('should render the card title', async () => {
    const { getByText } = renderComponentWithRouter(
      <ActivityCard namespace={namespaces.johnDoe} />,
    );

    expect(getByText('Recent activity')).toBeInTheDocument();

    await waitFor(() => {
      expect(getByText('No recent activity')).toBeInTheDocument();
    });
  });

  test('should render the empty state when the namespace has no recent activity', async () => {
    const { getByText } = renderComponentWithRouter(
      <ActivityCard namespace={namespaces.johnDoe} />,
    );

    await waitFor(() => {
      expect(getByText('No recent activity')).toBeInTheDocument();
    });
  });

  test('should render each record with its actor, message and relative timestamp', async () => {
    const { getByRole } = renderComponentWithRouter(
      <ActivityCard namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(getByRole('listitem', { name: audit.namespaceCreated.message })).toBeInTheDocument();
    });

    const namespaceItem = getByRole('listitem', { name: audit.namespaceCreated.message });
    const serviceItem = getByRole('listitem', { name: audit.serviceCreated.message });

    expect(within(namespaceItem).getByText(audit.namespaceCreated.actor.name)).toBeInTheDocument();
    expect(namespaceItem.querySelector('time')).toBeInTheDocument();
    expect(within(serviceItem).getByText(audit.serviceCreated.actor.name)).toBeInTheDocument();
  });

  test('should render an error state when the audit records request fails', async () => {
    server.use(
      http.get('http://localhost/api/namespaces/:slug/audit', () => (
        HttpResponse.json({
          status: 500,
          title: 'Internal server error',
          detail: 'Something went wrong while loading the recent activity.',
        }, { status: 500 })
      )),
    );

    const { getByText, queryByText } = renderComponentWithRouter(
      <ActivityCard namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(getByText('Internal server error')).toBeInTheDocument();
    });

    expect(queryByText('No recent activity')).not.toBeInTheDocument();
  });
});
