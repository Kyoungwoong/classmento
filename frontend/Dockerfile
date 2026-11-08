# syntax=docker/dockerfile:1
# classmento frontend (Next.js 16, standalone 출력)
# 빌드: docker build --build-arg BACKEND_URL=http://backend:8080 -t classmento-frontend ./frontend
# 주의: next.config.ts의 rewrites(/api → 백엔드)는 "빌드 시점"에 BACKEND_URL로 고정된다.
#       환경(스테이징/운영)마다 백엔드 주소가 다르면 이미지를 따로 빌드한다.

FROM node:24-alpine AS base
RUN corepack enable
WORKDIR /app

FROM base AS deps
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

FROM base AS build
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=${BACKEND_URL} \
    NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN pnpm build && mkdir -p public

FROM node:24-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
COPY --from=build --chown=app:app /app/public ./public
USER app
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD ["wget", "-q", "-O", "/dev/null", "http://127.0.0.1:3000/"]
CMD ["node", "server.js"]
