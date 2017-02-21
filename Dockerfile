FROM openjdk:8-jdk-alpine

MAINTAINER Emmanuel Keller

ADD target/qwazr-webapps-1.2.0-SNAPSHOT-exec.jar /usr/share/qwazr/qwazr-webapps.jar

VOLUME /var/lib/qwazr

EXPOSE 9090 9091

WORKDIR /var/lib/qwazr/

CMD ["java", "-Dfile.encoding=UTF-8", "-jar", "/usr/share/qwazr/qwazr-webapps.jar"]
