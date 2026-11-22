# Immagine del frontend (#100): Vite prepara i file, nginx li serve e passa al backend tutto
# quello che inizia con /api e /images (come fa Vite in sviluppo, guida per il frontend).
# Così frontend e backend sono sullo stesso indirizzo, come alla consegna.
#   docker build -t dreamscape-frontend sviluppo/frontend
# Di solito la costruisce Docker Compose (sviluppo/compose.yaml) insieme al resto.

# 1. Prepara i file del sito con Vite
FROM node:22-alpine AS build
WORKDIR /app
# Prima solo le dipendenze: restano nella cache finché package-lock.json non cambia
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 2. nginx serve i file preparati
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
