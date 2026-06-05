import { z } from 'zod';
import { createServerFn } from '@tanstack/react-start';

export const FetchConfigSchema = z.object({
  username: z.string().trim().min(1, { message: 'User name is required' }),
  password: z.string().trim().min(1, { message: 'Password is required' }),
  configServerUrl: z.string()
    .trim()
    .min(1, { message: 'Config server URL is required' })
    .url({ message: 'Config server URL must be a valid URL' }),
});

export type FetchConfigRequest = z.infer<typeof FetchConfigSchema>;

async function fetchSpringConfig({ data }: { data: FetchConfigRequest }) {
  const auth = btoa(`${data.username}:${data.password}`);
  const response = await fetch(data.configServerUrl, {
    method: 'GET',
    headers: {
      Authorization: `Basic ${auth}`,
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch configuration: ${response.status}`);
  }

  return await response.json();
}

export const fetchSpringConfigHandler = createServerFn({ method: 'GET' })
  .inputValidator(FetchConfigSchema)
  .handler(fetchSpringConfig);
