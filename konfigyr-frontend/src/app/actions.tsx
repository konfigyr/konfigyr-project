'use server';

import type { ContactInformation } from 'konfigyr/components/contact';

import { Resend, type CreateEmailOptions } from 'resend';
import purify from 'isomorphic-dompurify';

const resend = new Resend(process.env.RESEND_API_KEY);

function Template(information: ContactInformation) {
  const sanitized = purify.sanitize(information.message);

  return (
    <>
      <h3>A new contact form submission has been received.</h3>
      <p><strong>Subject</strong>: {information.subject}</p>
      <p><strong>Name</strong>: {information.name}</p>
      <p><strong>Email</strong>: {information.email}</p>
      <p><strong>Message</strong>:</p>
      <p>{sanitized}</p>
    </>
  );
}

async function send(information: ContactInformation) {
  const mail: CreateEmailOptions = {
    from: information.email,
    to: 'contact@konfigyr.com',
    subject: 'Konfigyr - Contact form request',
    react: Template(information),
  };

  const { data, error } = await resend.emails.send(mail);

  if (error) {
    throw new Error(`Failed to send contact email due to Resend error response: ${error}`);
  }

  console.log('Successfully sent contact request with email identifier:', data.id);
}

export async function sendContactInformation(information: ContactInformation): Promise<{ error: boolean }> {
  try {
    await send(information);
  } catch(error) {
    console.warn('Unexpected error occurred while sending contact information:', information, error);
    return { error: true };
  }

  return { error: false };
}
