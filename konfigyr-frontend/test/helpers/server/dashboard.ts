import { HttpResponse, http } from 'msw';
import * as namespaces from '../mocks/namespace';
import { johnDoeSummary, konfigyrSummary } from '../mocks/dashboard';

const summary = http.get('http://localhost/api/namespaces/:slug/dashboard', ({ params }) => {
  if (params.slug === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with slug '${params.slug}' not found.`,
    }, { status: 404 });
  }

  if (params.slug === namespaces.johnDoe.slug) {
    return HttpResponse.json(johnDoeSummary);
  }

  return HttpResponse.json(konfigyrSummary);
});

export default [
  summary,
];
