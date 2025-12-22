import { defineConfig } from 'vite';
import tsConfigPaths from 'vite-tsconfig-paths';
import { nitro } from 'nitro/vite';
import viteReact from '@vitejs/plugin-react-swc';
import { tanstackStart } from '@tanstack/react-start/plugin/vite';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  server: {
    port: 3000,
  },
  plugins: [
    tsConfigPaths({
      projects: ['./tsconfig.json'],
    }),
    tanstackStart(),
    nitro(),
    viteReact({
      tsDecorators: true,
      plugins: [
        [
          '@swc/plugin-formatjs',
          {
            idInterpolationPattern: '[md5:contenthash:hex:10]',
            ast: true,
          },
        ],
      ],
    }),
    tailwindcss(),
  ],
});
