FROM eclipse-temurin:25-jre

RUN groupadd --system --gid 1001 app \
 && useradd --system --uid 1001 --gid app --home-dir /app --no-create-home app

WORKDIR /app
COPY --chown=app:app finance-impl/target/finance.jar app.jar

USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
