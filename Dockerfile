FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY target/search-aggregator-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8090"]
