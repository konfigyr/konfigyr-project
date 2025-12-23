import { HttpResponse, http } from 'msw';
import { namespaces, services } from '../mocks';

const list = http.get('http://localhost/api/namespaces/:namespace/services', ({ params }) => {
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

const get = http.get('http://localhost/api/namespaces/:namespace/services/:service', ({ params }) => {
  const { namespace, service } = params;

  if (namespace !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    });
  }

  switch (service) {
    case services.konfigyrApi.slug:
      return HttpResponse.json(services.konfigyrApi);
    case services.konfigyrId.slug:
      return HttpResponse.json(services.konfigyrId);
    default:
      return HttpResponse.json({
        status: 404,
        title: 'Not found',
        detail: `Service with identifier '${service}' not found.`,
      }, { status: 404 });
  }
});

const create = http.post('http://localhost/api/namespaces/:namespace/services', async ({ params, request }) => {
  const { namespace } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  const body = await request.clone().json() as Record<string, unknown>;

  if (body.slug === 'invalid-service-identifier') {
    return HttpResponse.json({
      status: 400,
      title: 'Can not create service',
      detail: 'Service identifier is invalid.',
    }, { status: 400 });
  }

  return HttpResponse.json({
    id: body.slug,
    namespace: namespaces.konfigyr.id,
    ...body,
  }, { status: 201 });
});

export default [
  list,
  get,
  create,
];
