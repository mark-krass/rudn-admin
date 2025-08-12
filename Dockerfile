FROM bellsoft/liberica-openjdk-alpine:latest

# Install shells and SSH client
RUN apk update && apk add --no-cache bash openssh-client

COPY build/libs/rudn-admin-1.0-SNAPSHOT-application.jar rudn-admin-image.jar
ENTRYPOINT ["java", "-jar", "/rudn-admin-image.jar"]