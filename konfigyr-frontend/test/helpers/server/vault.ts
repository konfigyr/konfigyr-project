import { HttpResponse, http } from 'msw';
import {
  ChangeRequestMergeStatus,
  ChangeRequestState,
} from '@konfigyr/hooks/vault/types';
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
  } else if (namespace === namespaces.johnDoe.slug) {
    return HttpResponse.json({ data: [] });
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

const submitChangeset = http.post('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/submit', async ({ params, request }) => {
  const { namespace, service, profile } = params;

  const { subject, description, changes } = await request.clone().json() as {
    subject?: string,
    description?: string,
    changes: Array<any>,
  };

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
    id: 'submitted-changes',
    service: services.konfigyrApi,
    profile: profiles.staging,
    number: 9,
    state: ChangeRequestState.OPEN,
    mergeState: ChangeRequestMergeStatus.MERGEABLE,
    count: changes.length,
    subject: subject,
    description: description,
    createdBy: 'John Doe',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  });
});

const getHistory = http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/history', ({ params, request }) => {
  const uri = new URL(request.url);
  const { namespace, service, profile } = params;

  if (namespace !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  if (service !== services.konfigyrApi.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  if (profile !== profiles.development.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  // five days ago
  const appliedAt = new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString();
  return HttpResponse.json({
    data: [{
      id: '9eadce4691d8fcd863aeeb07ef81d8146083d814',
      revision: '9eadce4691d8fcd863aeeb07ef81d8146083d814',
      subject: 'Changeset draft',
      description: 'Changeset draft',
      changes: 4,
      appliedBy: 'Test User <test.user@ebf.com>',
      appliedAt,
    }],
    metadata: {
      size: uri.searchParams.get('size') || 20,
      previous: uri.searchParams.has('token') && 'previous-token',
      next: 'next-token',
    },
  });
});

const getHistoryDetails = http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/history/:revision', ({ params }) => {
  const { namespace, service, profile, revision } = params;

  if (namespace !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  if (service !== services.konfigyrApi.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  if (profile !== profiles.development.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  if (revision === 'empty-revision') {
    return HttpResponse.json({
      data: [],
    });
  }

  return HttpResponse.json({
    data: [{
      id: '1',
      name: 'spring.config.location',
      revision,
      action: 'UPDATED',
      from: 'file:/config.yaml',
      to: 'classpath:/config.yaml',
      appliedAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
      appliedBy: 'jane.doe@konfigyr.com',
    }, {
      id: '2',
      name: 'logging.level.com.konfigyr.test',
      revision,
      action: 'UPDATED',
      from: 'DEBUG',
      to: 'INFO',
      appliedAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
      appliedBy: 'jane.doe@konfigyr.com',
    }, {
      id: '3',
      name: 'logging.file.max-size',
      revision,
      action: 'ADDED',
      to: '10GB',
      appliedAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
      appliedBy: 'jane.doe@konfigyr.com',
    }, {
      id: '4',
      name: 'logging.file.max-history',
      revision,
      action: 'REMOVED',
      from: '10',
      appliedAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
      appliedBy: 'jane.doe@konfigyr.com',
    }],
  });
});

const getPropertyHistory = http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/property/:name/history', ({ params, request }) => {
  const uri = new URL(request.url);
  const { namespace, service, profile, name } = params;

  if (namespace !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  if (service !== services.konfigyrApi.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  if (profile !== profiles.development.slug) {
    return HttpResponse.json({
      data: [],
    });
  }

  if (name === 'empty-configuration-property') {
    return HttpResponse.json({
      data: [],
    });
  }

  return HttpResponse.json({
    data: [{
      id: '1',
      name,
      revision: '80b028f2248cbaa3b36cb4d615ee9c8d9384a8a6',
      action: 'UPDATED',
      from: '10',
      to: '20',
      appliedAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
      appliedBy: 'jane.doe@konfigyr.com',
    }, {
      id: '2',
      name,
      revision: '80b028f2248cbaa3b36cb4d615ee9c8d9384a8a6',
      action: 'UPDATED',
      from: '5',
      to: '10',
      appliedAt: new Date(Date.now() - 1000 * 60 * 60 * 3).toISOString(),
      appliedBy: 'john.doe@konfigyr.com',
    }, {
      id: '3',
      name,
      revision: '85698e54b9eb76912c58729703e1bf9ce0c6fad4',
      action: 'ADDED',
      to: '5',
      appliedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString(),
      appliedBy: 'jane.doe@konfigyr.com',
    }],
    metadata: {
      size: uri.searchParams.get('size') || 20,
      previous: uri.searchParams.has('token') && 'previous-token',
      next: 'next-token',
    },
  });
});

export default [
  getProfiles,
  getProfile,
  createProfile,
  history,
  getProperties,
  applyChangeset,
  submitChangeset,
  getHistory,
  getHistoryDetails,
  getPropertyHistory,
];
