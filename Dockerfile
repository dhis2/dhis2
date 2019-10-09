#
# Build the base DHIS2 image
#

FROM maven:3.6.2-jdk-8-slim as build
LABEL stage=intermediate

ARG IDENTIFIER=unknown
LABEL identifier=${IDENTIFIER}

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install --no-install-recommends -y \
        git && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /src

# NB: web-apps build uses `git rev-parse` to tag the build, so just copy over the whole tree for now
COPY dhis-2 .

# TODO: We should be able to achieve much faster incremental builds and cached dependencies using
RUN mvn clean install -T1C -f pom.xml -DskipTests
RUN mvn clean install -T1C -U -f dhis-web/pom.xml -DskipTests

RUN cp dhis-web/dhis-web-portal/target/dhis.war /dhis.war && \
    pushd / && \
    sha256sum dhis.war > /sha256sum.txt && \
    md5sum dhis.war > /md5sum.txt && \
    popd

#
# Slim final image that has the build artifacts at root-level
#

FROM alpine:latest
COPY --from=build /dhis.war /dhis.war
COPY --from=build /sha256sum.txt /sha256sum.txt
COPY --from=build /md5sum.txt /md5sum.txt
