'use server';

import { cookies } from 'next/headers';
import ServiceLabel from 'konfigyr/components/service-label';
import LandingPage from 'konfigyr/components/landing-page';
import { isAuthenticated } from 'konfigyr/services/authentication';
import { sendContactInformation } from './actions';

export default async function Home() {
  const authenticated = await isAuthenticated(await cookies());

  if (authenticated) {
    return (
      <ServiceLabel/>
    );
  }

  return (
    <LandingPage onContact={sendContactInformation} />
  );
}
