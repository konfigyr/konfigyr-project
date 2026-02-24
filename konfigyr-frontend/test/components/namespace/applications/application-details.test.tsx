import { afterEach, describe, expect, test, vi } from 'vitest';
import { applications, namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup } from '@testing-library/react';
import {NamespaceApplicationDetails} from '@konfigyr/components/namespace/applications/application-details';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';

describe('components | namespace | applications | <NamespaceApplicationDetails/>', () => {
  afterEach(() => cleanup());

  test('should render namespace applications details without action buttons', () => {
    const result = renderComponentWithRouter(
      <NamespaceApplicationDetails namespace={namespaces.konfigyr} namespaceApplication={applications.konfigyr} showActions={false} />,
    );

    expect(result.queryByText('Reset application')).not.toBeInTheDocument();
    expect(result.queryByText('Delete application')).not.toBeInTheDocument();
  });

  test('should render namespace applications details without client secret', () => {
    const result = renderComponentWithRouter(
      <NamespaceApplicationDetails namespace={namespaces.konfigyr} namespaceApplication={applications.konfigyr} showActions={true} />,
    );

    expect(result.getByText('Client ID:'), 'render client id').toBeInTheDocument();
    expect(result.getByText('Scopes:'), 'render scopes').toBeInTheDocument();
    expect(result.queryByText('Client Secret:'), 'not render client secret').not.toBeInTheDocument();

    expect(result.getByText('Reset application')).toBeInTheDocument();
    expect(result.getByText('Delete application')).toBeInTheDocument();
  });

  test('should render namespace applications details with client secret', () => {
    const result = renderComponentWithRouter(
      <NamespaceApplicationDetails namespace={namespaces.konfigyr}
        namespaceApplication={{
          ...applications.konfigyr,
          clientSecret: 'JMAm42MSA0I4_iwzH39Oex0N7qoqSb91z6jEp7LQwQ4',
        }}
      />,
    );

    expect(result.getByText('Client Secret:'), 'render client secret label').toBeInTheDocument();
    expect(result.getByText('JMAm42MSA0I4_iwzH39Oex0N7qoqSb91z6jEp7LQwQ4'), 'render client secret value').toBeInTheDocument();
    expect(result.getByText('Make sure to copy your client secret now as you will not be able to see this again.')).toBeInTheDocument();
  });


});
