FROM eclipse-temurin:25-jre

RUN groupadd --system --gid 1001 app \
 && useradd --system --uid 1001 --gid app --home-dir /app --no-create-home app

WORKDIR /app
COPY --chown=app:app identity-impl/target/identity.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
