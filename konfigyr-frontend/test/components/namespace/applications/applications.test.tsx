import { afterEach, describe, expect, test } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { applications, namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import {
  NamespaceApplicationArticle,
  NamespaceApplications,
} from '@konfigyr/components/namespace/applications/applications';

describe('components | namespace | applications | <NamespaceApplications/>', () => {
  afterEach(() => cleanup());

  test('should render namespace applications with loading state', () => {
    const result = renderComponentWithRouter(
      <NamespaceApplications namespace={namespaces.konfigyr} />,
    );

    expect(result.getByText('Namespace applications')).toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="namespace-application-skeleton"]')).toBeInTheDocument();
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
      expect(result.queryByText('konfigyr test')).toBeInTheDocument();
      expect(result.queryByText('This application has no expiration date')).toBeInTheDocument();
    });
  });

  test('should render <NamespaceApplicationArticle /> component  ', async () => {
    const { getByText } = renderComponentWithRouter(
      <NamespaceApplicationArticle namespace={namespaces.konfigyr} application={applications.konfigyr} />,
    );

    await waitFor(() => {
      expect(getByText(applications.konfigyr.name)).toBeInTheDocument();
      expect(getByText('This application has no expiration date')).toBeInTheDocument();
    });
  });
});
