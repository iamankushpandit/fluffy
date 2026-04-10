FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="fluffy-batch"
LABEL description="Fluffy Batch Example Application"

RUN addgroup -S fluffy && adduser -S fluffy -G fluffy

WORKDIR /app

COPY target/fluffy-batch-example-*.jar app.jar

RUN chown -R fluffy:fluffy /app

USER fluffy

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
