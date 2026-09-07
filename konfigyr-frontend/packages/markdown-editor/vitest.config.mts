import { createVitestConfig } from '@konfigyr/vitest-config';

export default createVitestConfig({
  react: {
    plugins: [
      [
        '@swc/plugin-formatjs',
        {
          idInterpolationPattern: '[sha512:contenthash:base64:10]',
          ast: true,
        },
      ],
    ],
  },
});
