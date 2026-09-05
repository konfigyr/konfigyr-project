import { afterEach, describe, expect, test } from 'vitest';
import { HttpResponse, http } from 'msw';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import { server } from '@konfigyr/test/helpers/server';
import { invitations } from '@konfigyr/test/helpers/mocks';

describe('routes | index', () => {
  afterEach(() => cleanup());

  test('should redirect user to first available Namespace from Account memebership', async () => {
    const { router } = renderWithRouter('/');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr');
    });
  });

  test('should redirect to namespace provisioning when the account has no memberships and no pending invitations', async () => {
    server.use(
      http.get('http://localhost/api/namespaces', () => HttpResponse.json({ data: [] })),
      http.get('http://localhost/api/account/invitations', () => HttpResponse.json({ data: [] })),
    );

    const { router } = renderWithRouter('/');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/provision');
    });
  });

  test('should redirect to namespace provisioning when every pending invitation has expired', async () => {
    server.use(
      http.get('http://localhost/api/namespaces', () => HttpResponse.json({ data: [] })),
      http.get('http://localhost/api/account/invitations', () => HttpResponse.json({
        data: [{ ...invitations.konfigyr, expired: true }],
      })),
    );

    const { router } = renderWithRouter('/');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/provision');
    });
  });

  test('should redirect directly to the join page when there is exactly one pending invitation', async () => {
    server.use(
      http.get('http://localhost/api/namespaces', () => HttpResponse.json({ data: [] })),
      http.get('http://localhost/api/account/invitations', () => HttpResponse.json({
        data: [invitations.konfigyr],
      })),
    );

    const { router, getByText } = renderWithRouter('/');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe(`/join/${invitations.konfigyr.key}`);
    });

    expect(getByText('You\'ve been invited')).toBeInTheDocument();
  });

  test('should redirect to the invitations list when there are multiple pending invitations', async () => {
    server.use(
      http.get('http://localhost/api/namespaces', () => HttpResponse.json({ data: [] })),
    );

    const { router, getByRole } = renderWithRouter('/');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/invitations');
    });

    expect(getByRole('heading', { name: 'Invitations' })).toBeInTheDocument();
  });

  test('should not redirect to namespace provisioning when fetching pending invitations fails', async () => {
    server.use(
      http.get('http://localhost/api/namespaces', () => HttpResponse.json({ data: [] })),
      http.get('http://localhost/api/account/invitations', () => (
        HttpResponse.json({
          status: 500,
          title: 'Internal server error',
          detail: 'Something went wrong while loading pending invitations.',
        }, { status: 500 })
      )),
    );

    const { router, getByText } = renderWithRouter('/');

    await waitFor(() => {
      expect(getByText('Internal server error')).toBeInTheDocument();
    });

    expect(router.state.location.pathname).not.toBe('/namespace/provision');
  });
});
