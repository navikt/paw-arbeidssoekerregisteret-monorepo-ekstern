FROM ghcr.io/navikt/baseimages/temurin:21
ENV APPLIKASJON_JAR=App.jar

ENV JAVA_OPTS="-XX:ActiveProcessorCount=4 -XX:+UseZGC -XX:+ZGenerational"

COPY build/libs/fat.jar app.jar
CMD ["java", "-jar", "$APPLIKASJON_JAR"]
