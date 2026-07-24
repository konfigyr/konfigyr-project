import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { dashboard, namespaces } from '@konfigyr/test/helpers/mocks';
import { server } from '@konfigyr/test/helpers/server';
import { DashboardStats } from '@konfigyr/components/reporting/dashboard-stats';

describe('components | reporting | <DashboardStats/>', () => {
  afterEach(() => cleanup());

  test('should render all 5 stat tiles with the members limit', async () => {
    const { getByText, getByRole, queryByText } = renderComponentWithRouter(
      <DashboardStats namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(getByText('Active services')).toBeInTheDocument();
    });

    expect(getByText('4')).toBeInTheDocument();
    expect(getByText('128')).toBeInTheDocument();
    expect(getByText('3')).toBeInTheDocument();
    expect(getByText('12 / 25')).toBeInTheDocument();
    expect(getByText('7')).toBeInTheDocument();
    expect(getByText('Artifacts owned')).toBeInTheDocument();
    expect(queryByText('12')).not.toBeInTheDocument();

    expect(getByText('Deployed here')).toBeInTheDocument();
    expect(getByText('Across 4 services')).toBeInTheDocument();
    expect(getByText('Awaiting review')).toBeInTheDocument();

    expect(getByRole('link', { name: /Browse artifacts/ }))
      .toHaveAttribute('href', '/namespace/konfigyr/artifactory/registry');
    expect(getByRole('link', { name: /Manage members/ }))
      .toHaveAttribute('href', '/namespace/konfigyr/members');
  });

  test('should render the members tile without a limit for unlimited plans', async () => {
    const { getByText, getByRole, queryByText } = renderComponentWithRouter(
      <DashboardStats namespace={namespaces.johnDoe} />,
    );

    await waitFor(() => {
      expect(getByText('5')).toBeInTheDocument();
    });

    expect(queryByText(/\//)).not.toBeInTheDocument();
    expect(getByRole('link', { name: /Manage members/ }))
      .toHaveAttribute('href', '/namespace/john-doe/members');
  });

  test('should show a positive message when there are no open change requests', async () => {
    server.use(
      http.get('http://localhost/api/namespaces/:slug/dashboard', () => (
        HttpResponse.json({ ...dashboard.konfigyrSummary, openChangeRequests: 0 })
      )),
    );

    const { getByText, queryByText } = renderComponentWithRouter(
      <DashboardStats namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(getByText('All caught up')).toBeInTheDocument();
    });

    expect(queryByText('Awaiting review')).not.toBeInTheDocument();
  });

  test('should use singular grammar when there is exactly one active service', async () => {
    server.use(
      http.get('http://localhost/api/namespaces/:slug/dashboard', () => (
        HttpResponse.json({ ...dashboard.konfigyrSummary, activeServices: 1 })
      )),
    );

    const { getByText, queryByText } = renderComponentWithRouter(
      <DashboardStats namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(getByText('Across 1 service')).toBeInTheDocument();
    });

    expect(queryByText('Across 1 services')).not.toBeInTheDocument();
  });

  test('should render an error state when the summary request fails', async () => {
    server.use(
      http.get('http://localhost/api/namespaces/:slug/dashboard', () => (
        HttpResponse.json({
          status: 500,
          title: 'Internal server error',
          detail: 'Something went wrong while loading the dashboard summary.',
        }, { status: 500 })
      )),
    );

    const { getByText, queryByText } = renderComponentWithRouter(
      <DashboardStats namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(getByText('Internal server error')).toBeInTheDocument();
    });

    expect(queryByText('Active services')).not.toBeInTheDocument();
  });
});
