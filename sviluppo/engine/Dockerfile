# Immagine di engine-service (decisione 029). Si costruisce da questa cartella:
#   docker build -t dreamscape-engine sviluppo/engine
# In sviluppo la costruisce Docker Compose (sviluppo/backend/compose.yaml) quando parte il backend.

# 1. Compila engine-core ed engine-service con il wrapper di Gradle
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle ./
COPY gradle gradle
# Prima solo Gradle e le build: le librerie scaricate restano nella cache anche quando cambia il codice
COPY engine-core/build.gradle engine-core/
COPY engine-service/build.gradle engine-service/
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -q help
COPY engine-core/src engine-core/src
COPY engine-service/src engine-service/src
# I test girano fuori dal container (./gradlew test): qui si costruisce solo il jar
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -q :engine-service:bootJar \
    && cp engine-service/build/libs/engine-service-*.jar engine.jar

# 2. Esegue il jar con il solo JRE, senza i sorgenti e senza l'utente root
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home engine
COPY --from=build /workspace/engine.jar engine.jar
USER engine
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "engine.jar"]
