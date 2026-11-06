FROM maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237 AS build

WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY src ./src
# Inside Docker the database host is the compose service "db".
RUN sed -i 's#localhost:5432#db:5432#' src/main/webapp/META-INF/context.xml
RUN mvn -B -ntp package

FROM postgres:17@sha256:2d2b8998d31037bf721cfdf764d76ba74171b4fab3431b7f72c27c56ddbdf9e3 AS postgres-init

# The image runs *.sql and *.sh here in name order on first start of an empty volume.
COPY src/main/resources/db/schema.sql /docker-entrypoint-initdb.d/01-schema.sql
COPY src/main/resources/db/demo-catalog.sql /docker-entrypoint-initdb.d/02-demo-catalog.sql

FROM tomcat:10.1.60-jdk21-temurin@sha256:887953afba0434879427f18d011d4df26d49e1f5c9aad18b779ed738edf22cf3 AS runtime

RUN command -v curl >/dev/null
# Compress text responses (HTML, CSS, JS, JSON); images and fonts are already compressed and are left alone.
RUN sed -i 's#<Connector port="8080" protocol="HTTP/1.1"#& compression="on"#' /usr/local/tomcat/conf/server.xml \
    && grep -q 'protocol="HTTP/1.1" compression="on"' /usr/local/tomcat/conf/server.xml
# The app is deployed as ROOT.war so it answers at "/" (no context path).
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /workspace/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
