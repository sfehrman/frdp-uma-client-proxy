# Dockerfile : UMA Client Proxy
#
# This dockerfile has two stages:
# 1: maven, gets source code and compiles/packages into web app
# 2: tomcat, copies web app, adds config
#
# Commands:
# docker build -t frdp-uma-client-proxy:1.2.0 .
# docker run --name client-proxy --rm -p 8095:8080 frdp-uma-client-proxy:1.2.0
# docker exec -it client-proxy /bin/bash
# docker login -u USER -p PASSWORD
# docker tag frdp-uma-client-proxy:1.2.0 USER/frdp-uma-client-proxy:1.2.0
# docker push USER/frdp-uma-client-proxy:1.2.0

# Get a container (maven) for compiling source code

FROM maven:3-openjdk-11 AS build

# Get the required projects from github

RUN git clone --branch 1.2.0 --progress --verbose https://github.com/ForgeRock/frdp-framework 

# run maven (mvn) to compile jar files and package the war file

WORKDIR /frdp-framework
RUN mvn compile install

RUN mkdir /frdp-uma-client-gateway
COPY ./gateway /frdp-uma-client-gateway

WORKDIR /frdp-uma-client-gateway
RUN mvn compile install

RUN mkdir /frdp-uma-client-proxy
COPY ./proxy /frdp-uma-client-proxy

WORKDIR /frdp-uma-client-proxy
RUN mvn compile package

# Get a container (tomcat) to run the application

FROM tomcat:9-jdk11-adoptopenjdk-hotspot

# Remove default applicatons

RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the expanded application folder

COPY --from=build /frdp-uma-client-proxy/target/uma-proxy /usr/local/tomcat/webapps/uma-proxy

EXPOSE 8095:8080

CMD ["catalina.sh", "run"]
