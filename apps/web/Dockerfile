# apps/web/Dockerfile -- build multi-stage: Node LTS compila, nginx:alpine sirve el estatico.
# Ver Modulo B, item 7 y Listado 1 de la Guia de Entrega 4.
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q -O- http://localhost/ || exit 1
