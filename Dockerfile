FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY checkout-service/pom.xml checkout-service/pom.xml
COPY integration-tests/pom.xml integration-tests/pom.xml

ARG MODULE
RUN mvn -q -pl ${MODULE} -am dependency:go-offline

COPY . .
RUN mvn -q -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:17-jre

ARG MODULE
ENV JAVA_OPTS=""
ENV SPRING_ZIPKIN_BASE_URL="http://zipkin:9411"
ENV SPRING_SLEUTH_SAMPLER_PROBABILITY="1.0"

WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
USER 10001

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
