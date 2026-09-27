# Build stage.
#
# VITE_API_BASE_URL is deliberately empty: the bundle then calls /api relative to whatever host
# served it, which is what this image's nginx.conf proxies to the API. Pass a URL only when the
# console is served from a different origin than the API, and remember to add that origin to the
# API's APP_ALLOWED_ORIGINS.
FROM node:22-alpine AS build
WORKDIR /workspace

ARG VITE_API_BASE_URL=
ARG VITE_DEMO_USERNAME=analyst
ARG VITE_DEMO_PASSWORD=cyclone-demo-analyst
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_DEMO_USERNAME=${VITE_DEMO_USERNAME} \
    VITE_DEMO_PASSWORD=${VITE_DEMO_PASSWORD}

COPY package.json package-lock.json ./
RUN npm ci --no-fund --no-audit

COPY . .
RUN npm run build

# Runtime stage: static files behind nginx.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost/ > /dev/null || exit 1
