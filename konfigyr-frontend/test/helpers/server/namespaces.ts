import { HttpResponse, http } from 'msw';
import { accounts, namespaces } from '../mocks';

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

  if (slug === namespaces.johnDoe.slug) {
    return HttpResponse.json(namespaces.johnDoe);
  }

  if (slug === namespaces.konfigyr.slug) {
    return HttpResponse.json(namespaces.konfigyr);
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${slug}' not found.`,
  }, { status: 404 });
});

const put = http.put('http://localhost/api/namespaces/:slug', async ({ params, request }) => {
  const { name, slug, description } = await request.clone().json() as {
    name: string,
    slug: string,
    description: string,
  };

  if (params.slug !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with slug '${slug}' not found.`,
    }, { status: 404 });
  }

  if (name !== namespaces.konfigyr.name) {
    return HttpResponse.json({
      status: 400,
      title: 'Bad request',
      detail: `Invalid namespace name: ${name}.`,
      errors: [{
        detail: 'Invalid namespace name.',
        pointer: 'name',
      }],
    }, { status: 400 });
  }

  if (slug !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 400,
      title: 'Bad request',
      detail: `Invalid namespace slug: ${slug}.`,
      errors: [{
        detail: 'Invalid namespace slug.',
        pointer: 'slug',
      }],
    }, { status: 400 });
  }

  if (description === 'Invalid description') {
    return HttpResponse.json({
      status: 400,
      title: 'Bad request',
      detail: `Invalid namespace description: ${description}.`,
      errors: [{
        detail: 'Invalid namespace description.',
        pointer: 'description',
      }],
    }, { status: 400 });
  }

  return HttpResponse.json(namespaces.konfigyr);
});

const deleteNamespace = http.delete('https://api.konfigyr.com/namespaces/:slug', ({ params }) => {
  const { slug } = params;

  if (slug === namespaces.konfigyr.slug || slug === namespaces.johnDoe.slug) {
    return new HttpResponse(null, { status: 204 });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace with slug '${slug}' not found.`,
  }, { status: 404 });
});

const inviteMember = http.post('http://localhost/api/namespaces/:slug/invitations', async ({ params, request }) => {
  const { slug } = params;
  const { email, role } = await request.clone().json() as Record<string, unknown>;

  if (slug !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with slug '${slug}' not found.`,
    }, { status: 404 });
  }

  if (email === 'invitee@konfigyr.com' && role === 'ADMIN') {
    return new HttpResponse(null, { status: 204 });
  }

  return HttpResponse.json({
    status: 400,
    title: 'Could not send invitation.',
    detail: `Could not send invitation to ${email}.`,
  }, { status: 400 });
});

const getMembers = http.get('http://localhost/api/namespaces/:slug/members', ({ params }) => {
  const { slug } = params;
  const members = [];

  if (slug === namespaces.konfigyr.slug) {
    members.push({
      id: accounts.johnDoe.id,
      role: 'ADMIN',
      email: accounts.johnDoe.email,
      fullName: accounts.johnDoe.fullName,
      avatar: accounts.johnDoe.avatar,
    });
    members.push({
      id: accounts.janeDoe.id,
      role: 'USER',
      email: accounts.janeDoe.email,
      fullName: accounts.janeDoe.fullName,
      avatar: accounts.janeDoe.avatar,
    });
  }

  return HttpResponse.json({ data: members });
});

const updateMember = http.put('http://localhost/api/namespaces/:slug/members/:member', async ({ params, request }) => {
  const { slug, member } = params;

  if (slug !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with slug '${slug}' not found.`,
    }, { status: 404 });
  }

  if (member === 'update-member') {
    const body = await request.clone().json() as Record<string, unknown>;
    return HttpResponse.json(body);
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace member with identifier '${member}' not found.`,
  }, { status: 404 });
});

const removeMember = http.delete('http://localhost/api/namespaces/:slug/members/:member', ({ params }) => {
  const { slug, member } = params;

  if (slug !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with slug '${slug}' not found.`,
    }, { status: 404 });
  }

  if (member === 'remove-member') {
    return new HttpResponse(null, { status: 204 });
  }

  return HttpResponse.json({
    status: 404,
    title: 'Not found',
    detail: `Namespace member with identifier '${member}' not found.`,
  }, { status: 404 });
});

export default [
  check,
  create,
  get,
  put,
  deleteNamespace,
  getMembers,
  inviteMember,
  updateMember,
  removeMember,
];
