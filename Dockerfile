FROM gradle:7.4.2-jdk17-alpine as BaseBuilder

LABEL org.opencontainers.image.source = https://github.com/Darkside138/DiscordSoundboard

COPY ./gradle/ /code/gradle/
COPY ./src/ /code/src/
COPY .gitignore build.gradle gradle.properties gradlew settings.gradle /code/

WORKDIR /code
RUN gradle assembleBootDist

WORKDIR build/distributions
RUN mkdir -p /app && cp DiscordSoundboard*.zip /app/DiscordSoundboard.zip

WORKDIR /app
RUN unzip DiscordSoundboard.zip
RUN rm DiscordSoundboard.zip


FROM bellsoft/liberica-openjdk-alpine:17.0.2-9

WORKDIR /app

COPY --from=BaseBuilder /app .

EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT [ "/docker-entrypoint.sh" ]
