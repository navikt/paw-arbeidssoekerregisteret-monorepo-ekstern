FROM microfrontend-toggler:unspecified
COPY ./build/agents/opentelemetry-javaagent-2.5.0.jar /app/agents/opentelemetry-javaagent.jar
ENTRYPOINT java -XX:ActiveProcessorCount=4 -XX:+UseZGC -XX:+ZGenerational -javaagent:/app/agents/opentelemetry-javaagent.jar -cp @/app/jib-classpath-file no.nav.paw.arbeidssoekerregisteret.ApplicationKt