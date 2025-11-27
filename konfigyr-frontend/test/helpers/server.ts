import { setupServer } from 'msw/node';
import { HttpResponse, http } from 'msw';

export const handlers = [
  http.get('https://api.konfigyr.com/data', () => {
    return HttpResponse.json({ answer: 42 });
  }),
];

export const server = setupServer(...handlers);
