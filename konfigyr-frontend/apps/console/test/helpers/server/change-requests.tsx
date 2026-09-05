import { HttpResponse, http } from 'msw';
import {
  ChangeRequestHistoryType,
  PropertyTransitionType,
} from '@konfigyr/hooks/vault/types';
import { subMinutes } from 'date-fns';
import { konfigyr } from '../mocks/namespace';
import { konfigyrApi, konfigyrId } from '../mocks/services';
import { firstChangeRequest, secondChangeRequest } from '../mocks/change-requests';

const list = http.get('http://localhost/api/namespaces/:namespace/services/:service/changes', ({ params }) => {
  if (params.namespace !== konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with slug '${params.namespace}' not found.`,
    }, { status: 404 });
  }

  if (params.service === konfigyrId.slug) {
    return HttpResponse.json({ data: [], metadata: { number: 1, size: 20, total: 0, pages: 0 } });
  }

  if (params.service !== konfigyrApi.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Service not found',
      detail: `Service with slug '${params.service}' not found.`,
    }, { status: 404 });
  }

  return HttpResponse.json({
    data: [firstChangeRequest, secondChangeRequest],
    metadata: { number: 1, size: 20, total: 2, pages: 1 },
  });
});

const withValidChangerRequest = (
  namespace: string,
  service: string,
  number: string,
  supplier: () => HttpResponse<any>,
) => {
  if (namespace !== konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with slug '${namespace}' not found.`,
    }, { status: 404 });
  }

  if (service !== konfigyrApi.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Service not found',
      detail: `Service with slug '${service}' not found.`,
    }, { status: 404 });
  }

  if (number !== '1') {
    return HttpResponse.json({
      status: 404,
      title: 'Change request not found',
      detail: `Change request with number '${number}' not found.`,
    }, { status: 404 });
  }

  return supplier();
};

const get = http.get('http://localhost/api/namespaces/:namespace/services/:service/changes/:number', ({ params }) => {
  return withValidChangerRequest(params.namespace as string, params.service as string, params.number as string, () => HttpResponse.json(
    firstChangeRequest,
  ));
});

const changes = http.get('http://localhost/api/namespaces/:namespace/services/:service/changes/:number/changes', ({ params }) => {
  return withValidChangerRequest(params.namespace as string, params.service as string, params.number as string, () => HttpResponse.json({
    data: [{
      name: 'logging.file.max-size',
      action: PropertyTransitionType.ADDED,
      to: '10GB',
    }, {
      name: 'logging.level.com.konfigyr.test',
      action: PropertyTransitionType.UPDATED,
      from: 'DEBUG',
      to: 'INFO',
    }, {
      name: 'logging.file.max-history',
      action: PropertyTransitionType.REMOVED,
      from: '10',
    }],
  }));
});

const history = http.get('http://localhost/api/namespaces/:namespace/services/:service/changes/:number/history', ({ params }) => {
  return withValidChangerRequest(params.namespace as string, params.service as string, params.number as string, () => HttpResponse.json({
    data: [{
      id: 'opened-event',
      type: ChangeRequestHistoryType.CREATED,
      initiator: 'Jane Doe',
      timestamp: subMinutes(new Date(), 12).toISOString(),
    }, {
      id: 'approved-event',
      type: ChangeRequestHistoryType.APPROVED,
      initiator: 'Jane Doe',
      timestamp: subMinutes(new Date(), 8).toISOString(),
    }],
  }));
});

const update = http.put('http://localhost/api/namespaces/:namespace/services/:service/changes/:number', async ({ params, request }) => {
  const { subject, description } = await request.clone().json() as {
    subject?: string,
    description?: string,
  };

  return withValidChangerRequest(params.namespace as string, params.service as string, params.number as string, () => HttpResponse.json({
    ...firstChangeRequest,
    subject: subject ?? firstChangeRequest.subject,
    description: {
      html: description ?? firstChangeRequest.description!.html,
      markdown: description ?? firstChangeRequest.description!.markdown,
    },
    updatedAt: new Date().toISOString(),
  }));
});

export default [
  list,
  get,
  changes,
  history,
  update,
];
