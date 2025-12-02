import { HttpResponse, http } from 'msw';

const check = http.head('http://localhost/api/namespaces/:slug', ({ params }) => {
  const status = params.slug === 'available-namespace' ? 404 : 200;
  return new HttpResponse(null, { status });
});

const create = http.post('http://localhost/api/namespaces', async ({ request }) => {
  const body= await request.clone().json() as Record<string, unknown>;

  return HttpResponse.json({ id: 'created-namespace', ...body }, { status: 201 });
});

const get = http.get('http://localhost/api/namespaces/:slug', ({ params }) => {
  const slug = params.slug;

  if (slug !== 'konfigyr') {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with slug '${slug}' not found.`,
    }, { status: 404 });
  }

  return HttpResponse.json({
    id: 'konfigyr',
    slug: 'konfigyr',
    name: 'Konfigyr namespace',
  }, { status: 200 });
});

export default [
  check,
  create,
  get,
];
