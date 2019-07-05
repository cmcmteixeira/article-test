FROM java:openjdk-8-jre-alpine
ARG PROJECT
ARG PROJECT_DIR
ARG BIN_FILE_NAME
WORKDIR /app
RUN apk add --no-cache curl tar bash
RUN curl -L "https://github.com/sbt/sbt/releases/download/v1.2.7/sbt-1.2.7.tgz" | tar -xz -C /root && \
    ln -s /root/sbt/bin/sbt /usr/local/bin/sbt && \
    chmod 0755 /usr/local/bin/sbt && \
    sbt sbtVersion
COPY . /app
RUN sbt ";clean;project ${PROJECT}; universal:packageBin"
RUN unzip -o ${PROJECT_DIR}/target/universal/${BIN_FILE_NAME}*.zip
RUN mkdir /build
RUN mv ${BIN_FILE_NAME}-*-SNAPSHOT /build/app
RUN mv /build/app/bin/${BIN_FILE_NAME} /build/app/bin/service

FROM java:openjdk-8-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app
COPY --from=0 /build/app /app
CMD "/app/bin/service"