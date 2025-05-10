import type { NextConfig } from 'next';
import createNextIntlPlugin from 'next-intl/plugin';

const nextConfig: NextConfig = {
  output: 'standalone',
  eslint: {
    dirs: ['src', '__tests__'],
  },
};

const withNextIntl = createNextIntlPlugin();
export default withNextIntl(nextConfig);
