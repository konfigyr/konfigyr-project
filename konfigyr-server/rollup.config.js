import {nodeResolve} from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import replace from '@rollup/plugin-replace';
import terser from '@rollup/plugin-terser';
import copy from 'rollup-plugin-copy'
import sass from 'rollup-plugin-sass';
import path from 'path';

const project = path.resolve('src/main/resources');
const assets = path.resolve('src/main/assets')
const dist = path.resolve('build/resources/dist');

export default {
    input: path.resolve(assets, 'scripts/main.js'),

    plugins: [
        nodeResolve({ browser: true }),
        commonjs(),
        terser(),
        sass({ output: true, includePaths: ['node_modules/'] }),
        replace({
            preventAssignment: true,
            'process.env.NODE_ENV': '"production"',
        }),
        copy({
            targets: [{
                src: 'node_modules/feather-icons/dist/feather-sprite.svg',
                dest: path.resolve(project, 'templates/fragments/'),
                rename: 'svg-symbols.html',
                transform: contents => `<div class="d-none" th:fragment="symbols">${contents.toString()}</div>`,
            }],
        }),
    ],

    output: {
        file: path.resolve(dist, 'konfigyr.js'),
        format: 'iife',
        sourcemap: true,
    }
};