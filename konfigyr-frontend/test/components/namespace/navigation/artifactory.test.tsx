import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { Layout } from '@konfigyr/components/layout';
import { NamespaceArtifactoryNavigationMenu } from '@konfigyr/components/namespace/navigation/artifactory';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | namespace | navigation | <NamespaceArtifactoryNavigationMenu/>', () => {
  afterEach(() => cleanup());

  test('should render a link to the artifact registry', () => {
    const { getByRole } = renderComponentWithRouter(
      <Layout>
        <NamespaceArtifactoryNavigationMenu namespace={namespaces.konfigyr}/>
      </Layout>,
    );

    expect(getByRole('link', { name: 'Artifact registry' }).getAttribute('href'))
      .toBe('/namespace/konfigyr/artifactory/registry');
  });

  test('should render the group verifications and ownership transfers links', () => {
    const { getByRole } = renderComponentWithRouter(
      <Layout>
        <NamespaceArtifactoryNavigationMenu namespace={namespaces.konfigyr}/>
      </Layout>,
    );

    expect(getByRole('link', { name: 'Group verifications' }).getAttribute('href'))
      .toBe('/namespace/konfigyr/artifactory/groups');
    expect(getByRole('link', { name: 'Ownership transfers' }).getAttribute('href'))
      .toBe('/namespace/konfigyr/artifactory/transfers');
  });
});
