import { HttpResponse, http } from 'msw';

const retrieve = http.get('http://localhost/api/account', () => {
  return HttpResponse.json({
    id: '06Y7W2BYKG9B9',
    email: 'john.doe@konfigyr.com',
    firstName: 'John',
    lastName: 'Doe',
    fullName: 'John Doe',
    avatar: 'https://avatar.vercel.sh/06Y7W2BYKG9B9.svg?text=VS',
    memberships: [],
  });
});

export default [
  retrieve,
];
