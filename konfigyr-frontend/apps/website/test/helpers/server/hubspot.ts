import { HttpResponse, http } from 'msw';

export default [
  http.post('https://api.hsforms.com/submissions/v3/integration/secure/submit/:portalId/:formGuid', () => {
    return HttpResponse.json({ inlineMessage: 'Thanks!' }, { status: 200 });
  }),
];
