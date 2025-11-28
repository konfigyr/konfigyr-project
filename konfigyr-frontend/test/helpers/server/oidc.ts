import * as jose from 'jose';
import { HttpResponse, http } from 'msw';

// generate the signing key for the JWT tokens
const key = await jose.generateKeyPair('RS256');

const metadata = http.get('https://id.konfigyr.com/.well-known/openid-configuration', () => {
  return HttpResponse.json({
    'issuer': 'https://id.konfigyr.com',
    'authorization_endpoint': 'https://id.konfigyr.com/oauth/authorize',
    'end_session_endpoint': 'https://id.konfigyr.com/oauth/endsession',
    'revocation_endpoint': 'https://id.konfigyr.com/oauth/revoke',
    'token_endpoint': 'https://id.konfigyr.com/oauth/token',
    'jwks_uri': 'https://id.konfigyr.com/oauth/jwks',
    'subject_types_supported': ['public'],
    'id_token_signing_alg_values_supported':[
      'RS256',
      'RS384',
      'RS512',
    ],
  });
});

const jwks = http.get('https://id.konfigyr.com/oauth/token', async () => {
  const jwk = await jose.exportJWK(key.publicKey);
  return HttpResponse.json({
    keys: [jwk],
  });
});

const token = http.post('https://id.konfigyr.com/oauth/token', async () => {
  const claims = {
    'sub': '1234567890',
    'email': 'john.doe@konfigyr.com',
  };

  const id = await new jose.SignJWT(claims)
    .setProtectedHeader({ alg: 'RS256' })
    .setIssuedAt()
    .setIssuer('https://id.konfigyr.com')
    .setAudience('konfigyr')
    .setExpirationTime('10m')
    .sign(key.privateKey);

  return HttpResponse.json({
    'token_type': 'Bearer',
    'access_token': 'access-token-jwt',
    'refresh_token': 'refresh-token',
    'id_token': id,
    'expires_in': 3600,
  });
});

export default [
  metadata,
  jwks,
  token,
];
