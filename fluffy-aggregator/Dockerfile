FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="fluffy-batch"
LABEL description="Fluffy Aggregator Service — aggregates job metrics from multiple Fluffy nodes"

RUN addgroup -S fluffy && adduser -S fluffy -G fluffy

WORKDIR /app

COPY target/fluffy-aggregator-*.jar app.jar

RUN chown -R fluffy:fluffy /app

USER fluffy

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
