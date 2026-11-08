FROM node:20-alpine AS build
WORKDIR /web
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# Imagem sem root: o nginx escuta na 8080 dentro do contêiner.
FROM nginxinc/nginx-unprivileged:1.27-alpine
ARG VITE_API_URL=http://localhost:8080
ENV API_URL=${VITE_API_URL}
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /web/dist /usr/share/nginx/html
EXPOSE 8080
