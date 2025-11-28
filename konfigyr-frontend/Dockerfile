FROM node:24-alpine AS development-dependencies-env
COPY . /app
WORKDIR /app
RUN npm ci

FROM node:24-alpine AS production-dependencies-env
COPY ./package.json package-lock.json /app/
WORKDIR /app
RUN npm ci --omit=dev

FROM node:24-alpine AS build-env
COPY . /app/
COPY --from=development-dependencies-env /app/node_modules /app/node_modules
WORKDIR /app
RUN npm run build

FROM node:24-alpine

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 tanstack
USER tanstack

COPY ./package.json package-lock.json /app/
COPY --from=production-dependencies-env /app/node_modules /app/node_modules
COPY --from=build-env /app/.output /app/

WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
EXPOSE 3000
CMD ["node", "./server/index.mjs"]
