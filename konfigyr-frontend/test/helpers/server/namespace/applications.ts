import { HttpResponse, http } from 'msw';
import { applications, namespaces } from '../../mocks';

const create = http.post('http://localhost/api/namespaces/:slug/applications', async ({ request }) => {
  const body= await request.clone().json() as Record<string, unknown>;

  return HttpResponse.json({
    id: 'created-application-id',
    clientSecret: 'created-secret',
    ...body ,
  }, { status: 201 });
});

const getAll = http.get('http://localhost/api/namespaces/:slug/applications', ({ params }) => {
  const { slug } = params;
  const result = [];

  if (slug === namespaces.konfigyr.slug) {
    result.push(applications.konfigyr);
  }

  return HttpResponse.json({ data: result });
});

const get = http.get('http://localhost/api/namespaces/:slug/applications/:id', ({ params }) => {
  const { slug, id } = params;

  if (slug === namespaces.konfigyr.slug && id === applications.konfigyr.id ) {
    return HttpResponse.json(applications.konfigyr);
  }

  if (slug === namespaces.konfigyr.slug && id === applications.createdApplication.id ) {
    return HttpResponse.json(applications.createdApplication);
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace member with identifier '${slug}' not found.`,
  }, { status: 404 });
});

const remove = http.delete('http://localhost/api/namespaces/:slug/applications/:id', ({ params }) => {
  const { slug, id } = params;

  if (slug !== namespaces.konfigyr.slug && id === 'existing-application-id') {
    return new HttpResponse(null, { status: 204 });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace application with identifier '${id}' not found.`,
  }, { status: 404 });
});

const reset = http.put('http://localhost/api/namespaces/:slug/applications/:id/reset', async ({ params }) => {
  const { id, slug } = params;

  if (slug !== namespaces.konfigyr.slug && id === 'existing-application-id') {
    return HttpResponse.json({
      ...applications.konfigyr,
      clientId: 'created-id',
      clientSecret: 'created-secret',
    });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace application with identifier '${id}' not found.`,
  }, { status: 404 });
});

export default [
  getAll,
  get,
  remove,
  reset,
  create,
];
