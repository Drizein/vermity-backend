FROM gradle:jdk22-jammy AS BUILD
WORKDIR /usr/app
COPY . .
RUN gradle build

FROM openjdk:22-jdk-bullseye

RUN apt-get update && apt-get install -y locales \
    && locale-gen de_DE.UTF-8 \
    && echo "LANG=de_DE.UTF-8" > /etc/default/locale \
    && echo "LANGUAGE=de_DE:de" >> /etc/default/locale \
    && echo "LC_ALL=de_DE.UTF-8" >> /etc/default/locale


ENV JAR_NAME=vermity-1.0.0.jar
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME
COPY --from=BUILD $APP_HOME .
ENTRYPOINT exec java -jar $APP_HOME/build/libs/$JAR_NAME
