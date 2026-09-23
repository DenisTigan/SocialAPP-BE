# Etapa 1: Construirea aplicației folosind Maven (Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiem fișierul pom.xml și descărcăm dependențele (ajută la cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiem sursele și compilăm proiectul (fără a rula testele)
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Rularea aplicației într-o imagine mai ușoară (JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiem doar fișierul .jar generat în prima etapă
COPY --from=build /app/target/*.jar app.jar

# Expunem portul pe care rulează Spring Boot
EXPOSE 8080

# Comanda de pornire a aplicației
ENTRYPOINT ["java", "-jar", "app.jar"]