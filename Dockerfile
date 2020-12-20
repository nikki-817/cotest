FROM openjdk:8
WORKDIR /
COPY src /src/
RUN javac -encoding UTF-8 -sourcepath ./src -d . ./src/com/company/*.java



