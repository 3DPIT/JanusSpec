FROM eclipse-temurin:21-jre-alpine


COPY build/libs/JanusSpec-*.jar app.jar

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app.jar"]
