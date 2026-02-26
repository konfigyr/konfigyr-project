import {afterEach, describe, expect, test, vi} from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import {applications, namespaces} from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import {
  NamespaceApplicationArticle,
  NamespaceApplications,
} from '@konfigyr/components/namespace/applications/applications';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';

describe('components | namespace | applications | <NamespaceApplications/>', () => {
  afterEach(() => cleanup());

  test('should render namespace applications with loading state', () => {
    const result = renderComponentWithRouter(
      <NamespaceApplications namespace={namespaces.konfigyr} />,
    );

    expect(result.getByText('Namespace applications')).toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="namespace-applications-skeleton"]')).toBeInTheDocument();
  });

  test('should render an empty namespace applications component', async () => {
    const result = renderComponentWithRouter(
      <NamespaceApplications namespace={namespaces.johnDoe} />,
    );

    await waitFor(() => {
      expect(result.getByText('Your namespace has no applications yet.')).toBeInTheDocument();
    });
  });

  test('should render namespace applications when loaded', async () => {
    const result = renderComponentWithRouter(
      <NamespaceApplications namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(result.container.querySelector('[data-slot="namespace-application-article"]')).toBeInTheDocument();
    });
  });

  test('should render <NamespaceApplicationArticle /> component  ', async () => {
    const onRemove = vi.fn();

    const { getByText, getByRole} = renderComponentWithRouter(
      <NamespaceApplicationArticle namespace={namespaces.konfigyr} application={applications.konfigyr} onRemove={onRemove}   />,
    );

    await waitFor(() => {
      expect(getByText('konfigyr test'), 'render name of the application').toBeInTheDocument();
      expect(getByText('This application has no expiration date'), 'render expiration date of the application').toBeInTheDocument();
      expect(getByText('Delete application')).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /delete application/i }),
    );

    expect(onRemove).toHaveBeenCalled();
  });
});
