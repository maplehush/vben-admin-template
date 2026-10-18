FROM node:22-slim AS builder

ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
ENV NODE_OPTIONS=--max-old-space-size=8192
ENV TZ=Asia/Shanghai
ENV CI=true

RUN npm i -g corepack

WORKDIR /app
COPY . /app
RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --frozen-lockfile
RUN pnpm run build:antdv-next

FROM nginx:stable-alpine AS production

COPY --from=builder /app/apps/web-antdv-next/dist /usr/share/nginx/html
COPY scripts/deploy/nginx.conf /etc/nginx/templates/default.conf.template

# Only replace this deployment value; Nginx's own $host, $uri, etc. must remain intact.
ENV NGINX_ENVSUBST_FILTER=^API_UPSTREAM$

EXPOSE 8080
