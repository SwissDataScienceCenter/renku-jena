## Licensed to the Apache Software Foundation (ASF) under one or more
## contributor license agreements.  See the NOTICE file distributed with
## this work for additional information regarding copyright ownership.
## The ASF licenses this file to You under the Apache License, Version 2.0
## (the "License"); you may not use this file except in compliance with
## the License.  You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.

## Apache Jena Fuseki server Dockerfile.

## This Dockefile builds a reduced footprint container.

ARG ALPINE_VERSION=3.19.1
ARG JENA_VERSION="5.0.0"
ARG JVM_ARGS="-Xmx2048m -Xms2048m"

# Internal, passed between stages.
ARG FUSEKI_HOME=/jena-fuseki
ARG FUSEKI_BASE=/fuseki

## ---- Stage: Download and build java.
FROM eclipse-temurin:21-jdk-alpine AS build-stage

ARG JENA_VERSION
ARG FUSEKI_HOME
ARG FUSEKI_BASE
ARG JVM_ARGS
ARG REPO=https://repo1.maven.org/maven2
ARG FUSEKI_TAR_NAME=apache-jena-fuseki-${JENA_VERSION}
ARG FUSEKI_TAR=${FUSEKI_TAR_NAME}.tar.gz
ARG TAR_URL=${REPO}/org/apache/jena/apache-jena-fuseki/${JENA_VERSION}/${FUSEKI_TAR}

RUN [ "${JENA_VERSION}" != "" ] || { echo -e '\n**** Set JENA_VERSION ****\n' ; exit 1 ; }
RUN echo && echo "==== Docker build for Apache Jena Fuseki ${JENA_VERSION} ====" && echo

# Alpine: For objcopy used in jlink
RUN apk add --no-cache curl tar binutils

## -- Fuseki binaries in FUSEKI_HOME.
WORKDIR /tmp

## -- Download the tar.gz file with check of the SHA1 checksum.
COPY download.sh .
RUN chmod a+x download.sh
RUN ./download.sh --chksum sha1 "${TAR_URL}"

## -- Extract the TAR and move files to $FUSEKI_HOME
RUN mkdir $FUSEKI_HOME && \
    tar zxf ${FUSEKI_TAR} && \
    mv ${FUSEKI_TAR_NAME}/* ${FUSEKI_HOME}

## -- Copying shiro.ini template for securing admin endpoints
COPY shiro.ini ${FUSEKI_HOME}/run/shiro.ini

## -- Copying config.ttl with the server settings
COPY config.ttl ${FUSEKI_HOME}/run/config.ttl

## -- Copying log4j2.properties
COPY log4j2.properties ${FUSEKI_HOME}/run/log4j2.properties

COPY /compactor/src/*.scala /compactor/src/
COPY /compactor/Compactor.scala /compactor/

RUN \
  apk add --no-cache wget coreutils && \
  wget -q -O scala-cli.gz https://github.com/Virtuslab/scala-cli/releases/latest/download/scala-cli-x86_64-pc-linux-static.gz && gunzip scala-cli.gz && \
  chmod +x scala-cli && \
  mv scala-cli /usr/bin/ && \
  scala-cli --power package /compactor/src /compactor/Compactor.scala -o /compactor/compact-jena --assembly

## -- Copying entrypoint.sh
COPY entrypoint.sh /

## ---- Stage: Build runtime
FROM alpine:${ALPINE_VERSION}

## Import ARGs
ARG JENA_VERSION
ARG FUSEKI_HOME
ARG FUSEKI_BASE
ARG JVM_ARGS
ARG ADMIN_USER
ARG SHIRO_INI_LOCATION
ARG COMPACTING_SCHEDULE

RUN apk add --no-cache curl tini bash openjdk21-jdk

COPY --from=build-stage $FUSEKI_HOME $FUSEKI_HOME
COPY --from=build-stage /etc/passwd /etc/passwd
COPY --from=build-stage /entrypoint.sh /entrypoint.sh
COPY --from=build-stage /compactor/compact-jena /usr/bin/compact-jena

WORKDIR ${FUSEKI_HOME}

## Creating 'fuseki' system user to be used for starting the service
# -D : no password
ENV GID=1000
RUN adduser --disabled-password -g "$GID" -D -u 1000 -s /bin/sh -h ${FUSEKI_HOME} fuseki

RUN \
    echo "#!/bin/sh" > .profile && \
    echo "alias ll='ls -l'" >> .profile && \
    mkdir ${FUSEKI_BASE} && \
    chown -R fuseki ${FUSEKI_BASE} && \
    chmod a+w ${FUSEKI_BASE} && \
    chown -R fuseki ${FUSEKI_HOME} && \
    chmod a+x /entrypoint.sh

## Default environment variables.
ENV \
    JVM_ARGS=${JVM_ARGS}          \
    JENA_VERSION=${JENA_VERSION}  \
    FUSEKI_HOME="${FUSEKI_HOME}"  \
    FUSEKI_BASE="${FUSEKI_BASE}"  \
    LOGGING="-Dlog4j.configurationFile=${FUSEKI_BASE}/log4j2.properties" \
    PATH="${FUSEKI_HOME}/bin:${PATH}" \
    ADMIN_USER="${ADMIN_USER}" \
    SHIRO_INI_LOCATION="${SHIRO_INI_LOCATION}" \
    COMPACTING_SCHEDULE="${COMPACTING_SCHEDULE}"

USER fuseki

RUN \
    rm -rf ${FUSEKI_BASE}/configuration/* && \
    rm -rf ${FUSEKI_BASE}/databases/*

VOLUME ${FUSEKI_BASE}

EXPOSE 3030
ENTRYPOINT ["/sbin/tini", "--", "/entrypoint.sh" ]
CMD ["${FUSEKI_HOME}/fuseki-server"]
