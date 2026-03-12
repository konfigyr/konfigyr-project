import { HttpResponse, http } from 'msw';
import {
  isValidProfile,
  isValidService,
  namespaces,
  profiles,
  services,
} from '../mocks';

const getProfiles = http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles', ({ params }) => {
  const { namespace, service } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  const data = [
    profiles.development,
    profiles.staging,
  ].map(profile => ({
    service, ...profile,
  }));

  return HttpResponse.json({ data });
});

const getProfile = http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile', ({ params }) => {
  const { namespace, profile } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  if (profile === profiles.development.slug) {
    return HttpResponse.json(profiles.development);
  }

  if (profile === profiles.staging.slug) {
    return HttpResponse.json(profiles.staging);
  }

  return HttpResponse.json({
    status: 404,
    title: 'Profile not found',
    detail: `Profile with identifier '${profile}' not found.`,
  }, { status: 404 });
});

const createProfile = http.post('http://localhost/api/namespaces/:namespace/services/:service/profiles', async ({ params, request }) => {
  const { namespace, service } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  const body = await request.clone().json() as Record<string, unknown>;

  if (body.slug === 'invalid-profile-identifier') {
    return HttpResponse.json({
      status: 400,
      title: 'Can not create profile',
      detail: 'Profile identifier is invalid.',
    }, { status: 400 });
  }

  return HttpResponse.json({
    id: body.slug,
    service,
    ...body,
  }, { status: 201 });
});

const history = http.get('http://localhost/api/namespaces/:namespace/services', ({ params }) => {
  const { namespace } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  const data = namespace === namespaces.konfigyr.slug ? Object.values(services) : [];

  return HttpResponse.json({ data });
});

const getProperties = http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/properties', ({ params }) => {
  const { namespace, service, profile } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  if (!isValidService(service)) {
    return HttpResponse.json({
      status: 404,
      title: 'Service not found',
      detail: `Service with identifier '${service}' not found.`,
    }, { status: 404 });
  }

  if (!isValidProfile(profile)) {
    return HttpResponse.json({
      status: 404,
      title: 'Profile not found',
      detail: `Profile with identifier '${profile}' not found.`,
    }, { status: 404 });
  }

  return HttpResponse.json({
    data: [{
      name: 'application.name',
      description: 'Application name property',
      type: 'java.lang.String',
      state: 'modified',
      value: 'konfigyr-frontend',
      schema: {
        type: 'string',
      },
    }, {
      name: 'application.profile',
      description: 'Application profile property',
      type: 'java.lang.String',
      state: 'unchanged',
      value: 'staging',
      deprecation: {
        reason: 'This property is deprecated',
      },
      schema: {
        type: 'string',
        enum: ['staging', 'production'],
      },
    }],
  });
});

const applyChangeset = http.post('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/apply', ({ params }) => {
  const { namespace, service, profile } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  if (!isValidService(service)) {
    return HttpResponse.json({
      status: 404,
      title: 'Service not found',
      detail: `Service with identifier '${service}' not found.`,
    }, { status: 404 });
  }

  if (!isValidProfile(profile)) {
    return HttpResponse.json({
      status: 404,
      title: 'Profile not found',
      detail: `Profile with identifier '${profile}' not found.`,
    }, { status: 404 });
  }

  return HttpResponse.json({
    revision: 'revision-hash',
    changes: [],
  });
});

export default [
  getProfiles,
  getProfile,
  createProfile,
  history,
  getProperties,
  applyChangeset,
];
