import { defineConfig } from 'vite';
import { nitro } from 'nitro/vite';
import viteReact from '@vitejs/plugin-react-swc';
import { tanstackStart } from '@tanstack/react-start/plugin/vite';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  server: {
    port: 3000,
  },
  resolve: {
    tsconfigPaths: true,
  },
  plugins: [
    tanstackStart(),
    nitro(),
    viteReact({
      tsDecorators: true,
      plugins: [
        [
          '@swc/plugin-formatjs',
          {
            idInterpolationPattern: '[sha512:contenthash:base64:10]',
            ast: true,
          },
        ],
      ],
    }),
    tailwindcss(),
  ],
});
