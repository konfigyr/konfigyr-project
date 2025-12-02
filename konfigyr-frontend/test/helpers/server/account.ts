import { HttpResponse, http } from 'msw';
import { janeDoe, johnDoe } from '../mocks/account';

const retrieve = http.get('http://localhost/api/account', () => {
  return HttpResponse.json({
    ...johnDoe,
    memberships: [],
  });
});

const updateDisplayName = http.patch('http://localhost/api/account', async ({ request }) => {
  const { name } = await request.clone().json() as { name: string };

  if (name === johnDoe.fullName) {
    return HttpResponse.json(johnDoe, { status: 200 });
  }

  if (name === janeDoe.fullName) {
    return HttpResponse.json(janeDoe, { status: 200 });
  }

  return HttpResponse.json({
    status: 400,
    title: 'Bad request',
    detail: `Invalid display name: ${name}.`,
    errors: [{
      detail: 'Invalid display name.',
      pointer: 'name',
    }],
  }, { status: 400 });
});

const updateEmailAddress = http.post('http://localhost/api/account/email', async ({ request }) => {
  const { email } = await request.clone().json() as { email: string };

  if (email === 'valid-address@konfigyr.com') {
    return HttpResponse.json({
      token: 'verification-token',
    }, { status: 200 });
  }

  return HttpResponse.json({
    status: 400,
    title: 'Bad request',
    detail: `Invalid email address: ${email}.`,
    errors: [{
      detail: 'Address is already used by another account.',
      pointer: 'email',
    }],
  }, { status: 400 });
});

const confirmEmailAddress = http.put('http://localhost/api/account/email', async ({ request }) => {
  const { token, code } = await request.clone().json() as { token: string, code: string };

  if (token === 'verification-token' && code === 'valid-code') {
    return HttpResponse.json(johnDoe, { status: 200 });
  }

  return HttpResponse.json({
    status: 400,
    title: 'Bad request',
    detail: 'Invalid verification code.',
  }, { status: 400 });
});

const deleteAccount = http.delete('https://api.konfigyr.com/account', () => {
  return new HttpResponse(null, { status: 204 });
});

export default [
  retrieve,
  updateDisplayName,
  updateEmailAddress,
  confirmEmailAddress,
  deleteAccount,
];
