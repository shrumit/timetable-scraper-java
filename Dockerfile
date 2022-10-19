FROM maven:3-openjdk-17
# change timezone to ET
RUN mv /etc/localtime /etc/localtime.bkp
RUN ln -s /usr/share/zoneinfo/America/Toronto /etc/localtime

WORKDIR /app
COPY . .
RUN mvn clean package
WORKDIR /execution

ENTRYPOINT ["java", "-jar", "/app/target/timetable-scraper-java-1.0-SNAPSHOT-jar-with-dependencies.jar"]
