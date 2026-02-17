import { HttpResponse, http } from 'msw';
import { namespaces, profiles, services } from '../mocks';

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

export default [
  getProfiles,
  getProfile,
  createProfile,
  history,
];
