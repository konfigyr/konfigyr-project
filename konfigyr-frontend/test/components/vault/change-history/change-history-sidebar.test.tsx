import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { ChangeHistorySidebar } from '@konfigyr/components/vault/change-history/change-history-sidebar';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';

const history = {
  id: 'first-change',
  revision: '9eadce4691d8fcd863aeeb07ef81d8146083d814',
  subject: 'First change',
  description: 'The first change to the configuration',
  count: 2,
  appliedBy: 'John Doe',
  appliedAt: new Date(Date.now() - 1000 * 60 * 60 * 3).toISOString(),
};

describe('components | vault | change-history | <ChangeHistorySidebar/>', () => {
  afterEach(() => cleanup());

  test('should render a closed history sidebar', () => {
    const { queryByRole } = renderComponentWithRouter(
      <ChangeHistorySidebar
        history={history}
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        profile={profiles.development}
      />,
    );

    expect(queryByRole('dialog', { name: history.subject })).toBeNull();
  });

  test('should render an opened history sidebar', () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <ChangeHistorySidebar
        open={true}
        history={history}
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        profile={profiles.development}
      />,
    );

    expect(getByRole('dialog', { name: history.subject })).toBeInTheDocument();
    expect(getByText(history.revision)).toBeInTheDocument();
    expect(getByText(history.description)).toBeInTheDocument();
    expect(getByText(history.appliedBy)).toBeInTheDocument();
  });

  test('should render history sidebar in a loading state', () => {
    const { baseElement: container } = renderComponentWithRouter(
      <ChangeHistorySidebar
        open={true}
        history={history}
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        profile={profiles.development}
      />,
    );

    expect(container.querySelector('[data-slot="changeset-history-skeleton"]'))
      .toBeInTheDocument();
  });

  test('should render history sidebar in an empty state', async () => {
    const { getByText } = renderComponentWithRouter(
      <ChangeHistorySidebar
        open={true}
        history={history}
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        profile={profiles.staging}
      />,
    );

    await waitFor(() => {
      expect(getByText('No property changes found')).toBeInTheDocument();
    });
  });

  test('should render history sidebar with loaded property history', async () => {
    const { getAllByRole, getByText } = renderComponentWithRouter(
      <ChangeHistorySidebar
        open={true}
        history={history}
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        profile={profiles.development}
      />,
    );

    await waitFor(() => {
      expect(getAllByRole('listitem')).length(3);
    });

    expect(getByText('spring.config.location')).toBeInTheDocument();
    expect(getByText('logging.level.com.konfigyr.test')).toBeInTheDocument();
    expect(getByText('logging.file.max-size')).toBeInTheDocument();
    expect(getByText('logging.file.max-history')).toBeInTheDocument();
  });
});
