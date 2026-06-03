FROM maven:3.9-eclipse-temurin-17-alpine
WORKDIR /app

# Cache de dependências (camada separada para reuso)
COPY pom.xml ./
COPY .mvn .mvn
RUN mvn dependency:go-offline -DskipTests

# Código fonte
COPY src src

# Hot reload via DevTools
CMD ["mvn", "spring-boot:run"]
