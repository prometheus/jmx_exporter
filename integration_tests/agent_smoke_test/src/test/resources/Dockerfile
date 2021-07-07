# Placeholders are replaced programmatically in JavaVersionsIT.
FROM ${base.image}
RUN mkdir -p /app
COPY ${agent.jar} /app/
COPY ${example_application.jar} /app/
COPY config.yml /app/
WORKDIR /app/
CMD echo "java -javaagent:${agent.jar}=9000:config.yml -jar ${example_application.jar}" && \
    java -javaagent:${agent.jar}=9000:config.yml -jar ${example_application.jar}