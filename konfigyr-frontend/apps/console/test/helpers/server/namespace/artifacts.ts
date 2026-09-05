import { HttpResponse, http } from 'msw';
import { namespaces } from '../../mocks';
import {
  platformSearchProperties,
  privateArtifact,
  publicArtifact,
  publicArtifactProperties,
  publicArtifactVersionOne,
  publicArtifactVersionTwo,
  singleVersion,
  singleVersionArtifact,
} from '../../mocks/artifacts';

import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
import type { ArtifactDefinition, PropertyDefinition, VersionedArtifact } from '@konfigyr/hooks/artifactory/types';

const artifacts = new Map<string, ArtifactDefinition>([
  [`${publicArtifact.groupId}:${publicArtifact.artifactId}`, publicArtifact],
  [`${privateArtifact.groupId}:${privateArtifact.artifactId}`, privateArtifact],
  [`${singleVersionArtifact.groupId}:${singleVersionArtifact.artifactId}`, singleVersionArtifact],
]);

const versionsByArtifact = new Map<string, Array<VersionedArtifact>>([
  [`${publicArtifact.groupId}:${publicArtifact.artifactId}`, [publicArtifactVersionTwo, publicArtifactVersionOne]],
  [`${privateArtifact.groupId}:${privateArtifact.artifactId}`, []],
  [`${singleVersionArtifact.groupId}:${singleVersionArtifact.artifactId}`, [singleVersion]],
]);

const notFound = (detail: string) => HttpResponse.json({ status: 404, title: 'Not found', detail }, { status: 404 });

const matchesTerm = (artifact: ArtifactDefinition, term: string) => {
  const value = term.toLowerCase();
  return artifact.groupId.toLowerCase().includes(value)
    || artifact.artifactId.toLowerCase().includes(value)
    || (artifact.name?.toLowerCase().includes(value) ?? false);
};

const list = http.get('http://localhost/api/namespaces/:slug/artifacts', ({ params, request }) => {
  const { slug } = params;
  const term = new URL(request.url).searchParams.get('term');

  let data = slug === namespaces.konfigyr.slug ? Array.from(artifacts.values()) : [];

  if (term) {
    data = data.filter(artifact => matchesTerm(artifact, term));
  }

  const response: PageResponse<ArtifactDefinition> = {
    data,
    metadata: { number: 1, size: 20, total: data.length, pages: 1 },
  };

  return HttpResponse.json(response);
});

const get = http.get('http://localhost/api/namespaces/:slug/artifacts/:groupId/:artifactId', ({ params }) => {
  const { slug, groupId, artifactId } = params;
  const artifact = artifacts.get(`${groupId}:${artifactId}`);

  if (slug !== namespaces.konfigyr.slug || !artifact) {
    return notFound(`Could not find artifact '${groupId}:${artifactId}'.`);
  }

  return HttpResponse.json(artifact);
});

const versions = http.get('http://localhost/api/namespaces/:slug/artifacts/:groupId/:artifactId/versions', ({ params }) => {
  const { slug, groupId, artifactId } = params;
  const key = `${groupId}:${artifactId}`;

  if (slug !== namespaces.konfigyr.slug || !artifacts.has(key)) {
    return notFound(`Could not find artifact '${key}'.`);
  }

  const data = versionsByArtifact.get(key) || [];

  const response: PageResponse<VersionedArtifact> = {
    data,
    metadata: { number: 1, size: 20, total: data.length, pages: 1 },
  };

  return HttpResponse.json(response);
});

const getVersion = http.get('http://localhost/api/namespaces/:slug/artifacts/:groupId/:artifactId/:version', ({ params }) => {
  const { slug, groupId, artifactId, version } = params;
  const key = `${groupId}:${artifactId}`;
  const found = (versionsByArtifact.get(key) || []).find(v => v.version === version);

  if (slug !== namespaces.konfigyr.slug || !found) {
    return notFound(`Could not find artifact version '${key}:${version}'.`);
  }

  return HttpResponse.json(found);
});

const changeVisibility = http.put('http://localhost/api/namespaces/:slug/artifacts/:groupId/:artifactId/visibility', async ({ params, request }) => {
  const { slug, groupId, artifactId } = params;
  const key = `${groupId}:${artifactId}`;
  const artifact = artifacts.get(key);

  if (slug !== namespaces.konfigyr.slug || !artifact) {
    return notFound(`Could not find artifact '${key}'.`);
  }

  const body = await request.clone().json() as { visibility?: ArtifactDefinition['visibility'] };
  artifacts.set(key, { ...artifact, visibility: body.visibility! });

  return new HttpResponse(null, { status: 204 });
});

const matchesPropertyTerm = (property: { name: string; description?: string }, term: string | null) => (
  !term || term.trim().length === 0
  || property.name.toLowerCase().includes(term.toLowerCase())
  || (property.description?.toLowerCase().includes(term.toLowerCase()) ?? false)
);

const search = http.get('http://localhost/api/namespaces/:slug/artifacts/search', ({ params, request }) => {
  const { slug } = params;
  const url = new URL(request.url);
  const term = url.searchParams.get('term');
  const groupId = url.searchParams.get('groupId');
  const artifactId = url.searchParams.get('artifactId');
  const version = url.searchParams.get('version');

  if (!groupId && !artifactId) {
    const data: Array<PropertyDefinition> = platformSearchProperties
      .filter(property => property.visibility === 'PUBLIC' || property.owner.slug === slug)
      .filter(property => matchesPropertyTerm(property, term))
      .map(({ visibility: _visibility, ...property }) => property);

    const response: PageResponse<PropertyDefinition> = {
      data,
      metadata: { number: 1, size: 20, total: data.length, pages: 1 },
    };

    return HttpResponse.json(response);
  }

  let data = groupId === publicArtifact.groupId && artifactId === publicArtifact.artifactId
    && (!version || version === publicArtifactVersionTwo.version)
    ? publicArtifactProperties
    : [];

  data = data.filter(property => matchesPropertyTerm(property, term));

  const response: PageResponse<PropertyDefinition> = {
    data,
    metadata: { number: 1, size: 20, total: data.length, pages: 1 },
  };

  return HttpResponse.json(response);
});

export default [
  list,
  get,
  versions,
  getVersion,
  changeVisibility,
  search,
];
