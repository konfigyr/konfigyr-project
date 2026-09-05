import { HttpResponse, http } from 'msw';
import { namespaces } from '../../mocks';
import {
  activeConflictedVerification,
  activeVerification,
  conflictedChallenge,
  conflictedVerification,
  pendingVerification,
  verificationChallenge,
} from '../../mocks/group-verifications';

import type { CollectionResponse, PageResponse } from '@konfigyr/hooks/hateoas/types';
import type { GroupVerification, VerificationChallenge, VerificationMethod } from '@konfigyr/hooks/groups/types';

const list = http.get('http://localhost/api/namespaces/:slug/group-verifications', ({ params, request }) => {
  const { slug } = params;
  const state = new URL(request.url).searchParams.get('state');

  let data = slug === namespaces.konfigyr.slug
    ? [activeVerification, pendingVerification, activeConflictedVerification]
    : [];

  if (state) {
    data = data.filter(verification => verification.state === state);
  }

  const response: PageResponse<GroupVerification> = {
    data,
    metadata: {
      number: 1,
      size: 20,
      total: data.length,
      pages: 1,
    },
  };

  return HttpResponse.json(response);
});

const get = http.get('http://localhost/api/namespaces/:slug/group-verifications/:groupId', ({ params }) => {
  const { slug, groupId } = params;

  if (slug !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Group verification claim with identifier '${groupId}' not found.`,
    }, { status: 404 });
  }

  if (groupId === activeVerification.groupId) {
    return HttpResponse.json(activeVerification);
  }

  if (groupId === pendingVerification.groupId) {
    return HttpResponse.json(pendingVerification);
  }

  if (groupId === conflictedVerification.groupId) {
    return HttpResponse.json(conflictedVerification);
  }

  if (groupId === activeConflictedVerification.groupId) {
    return HttpResponse.json(activeConflictedVerification);
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Group verification claim with identifier '${groupId}' not found.`,
  }, { status: 404 });
});

const challenges = http.get('http://localhost/api/namespaces/:slug/group-verifications/:groupId/challenges', ({ params }) => {
  const { slug, groupId } = params;

  if (slug !== namespaces.konfigyr.slug) {
    const response: CollectionResponse<VerificationChallenge> = { data: [] };
    return HttpResponse.json(response);
  }

  if (groupId === conflictedVerification.groupId) {
    const response: CollectionResponse<VerificationChallenge> = {
      data: [conflictedChallenge],
    };
    return HttpResponse.json(response);
  }

  if (groupId !== pendingVerification.groupId) {
    const response: CollectionResponse<VerificationChallenge> = { data: [] };
    return HttpResponse.json(response);
  }

  const response: CollectionResponse<VerificationChallenge> = {
    data: [verificationChallenge],
  };

  return HttpResponse.json(response);
});

const create = http.post('http://localhost/api/namespaces/:slug/group-verifications', async ({ params, request }) => {
  const { slug } = params;

  if (slug !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with slug '${slug}' not found.`,
    }, { status: 404 });
  }

  const body = await request.clone().json() as {
    groupId?: string;
    verificationMethod?: VerificationMethod;
  };

  const response: GroupVerification = {
    id: `${body.groupId}-claim`,
    groupId: body.groupId!,
    state: 'PENDING',
    createdAt: '2026-07-10T00:00:00Z',
    verifiedAt: null,
    revokedAt: null,
    ...(body.groupId === conflictedVerification.groupId
      ? { conflictingOwners: conflictedVerification.conflictingOwners }
      : {}),
  };

  return HttpResponse.json(response, { status: 201 });
});

export default [
  list,
  get,
  challenges,
  create,
];
