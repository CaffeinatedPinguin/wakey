FROM eclipse-temurin:25-jre-alpine@sha256:69c2a26e9894016e274c880fa542956d005b3755f84db765145185e87405bd89

# Unprivileged user; the default HSQLDB file lives in ./data under the workdir.
RUN addgroup -S wakey \
    && adduser -S -G wakey wakey \
    && mkdir -p /app/data \
    && chown -R wakey:wakey /app

WORKDIR /app
COPY --chown=wakey:wakey dist/*.jar /app/wakey-server.jar

USER wakey
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/wakey-server.jar"]
