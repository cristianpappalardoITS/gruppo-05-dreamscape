# Immagine del backend (#100). Si costruisce dalla cartella sviluppo/, non da sviluppo/backend/:
# il backend include il progetto del motore (settings.gradle, includeBuild '../engine') e usa le
# classi di engine-core, quindi servono tutte e due le cartelle.
#   docker build -f sviluppo/backend/Dockerfile -t dreamscape-backend sviluppo
# Di solito la costruisce Docker Compose (sviluppo/compose.yaml) insieme al resto.

# 1. Compila il jar del backend con il wrapper di Gradle
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
# Prima solo Gradle e le build (backend e motore): le librerie scaricate restano nella cache
# anche quando cambia il codice
COPY engine/gradlew engine/settings.gradle engine/
COPY engine/gradle engine/gradle
COPY engine/engine-core/build.gradle engine/engine-core/
COPY engine/engine-service/build.gradle engine/engine-service/
COPY engine/engine-simulation/build.gradle engine/engine-simulation/
COPY backend/gradlew backend/settings.gradle backend/build.gradle backend/
COPY backend/gradle backend/gradle
WORKDIR /workspace/backend
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -q dependencies > /dev/null
# Poi il codice: del motore serve solo engine-core
WORKDIR /workspace
COPY engine/engine-core/src engine/engine-core/src
COPY backend/src/main backend/src/main
WORKDIR /workspace/backend
# I test girano fuori dal container (./gradlew test, con Testcontainers): qui si costruisce solo il jar.
# Si copia il jar eseguibile, non quello "plain" senza le librerie
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -q bootJar \
    && find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

# 2. Esegue il jar con il solo JRE, senza i sorgenti e senza l'utente root
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home dreamscape \
    && mkdir -p /app/data/images /app/data/originals \
    && chown -R dreamscape /app/data
COPY --from=build /workspace/app.jar app.jar
USER dreamscape
# Illustrazioni caricate dal backoffice (IMAGES_DIR) e originali (ORIGINALS_DIR): in un volume
ENV IMAGES_DIR=/app/data/images \
    ORIGINALS_DIR=/app/data/originals
VOLUME /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
