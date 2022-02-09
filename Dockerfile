FROM maven:3-openjdk-17
WORKDIR /app
COPY . .
RUN mvn clean package
WORKDIR /execution
ENTRYPOINT java -jar /app/target/timetable-scraper-java-1.0-SNAPSHOT-jar-with-dependencies.jar
