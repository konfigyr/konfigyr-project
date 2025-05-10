import { getRequestConfig } from 'next-intl/server';
import { resolve } from './index';

export default getRequestConfig(async () => await resolve());
