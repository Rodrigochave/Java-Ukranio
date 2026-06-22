# 1. Definimos la imagen base con Java preinstalado
FROM eclipse-temurin:21-jre-jammy

# 2. Creamos un directorio de trabajo dentro del contenedor
WORKDIR /app

# 3. Copiamos el archivo JAR desde tu computadora hacia el contenedor
COPY target/my-app-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

# 4. Informamos qué puerto utiliza la API REST
EXPOSE 8080

# 5. El comando que se ejecutará automáticamente al iniciar el contenedor
ENTRYPOINT ["java", "-jar", "app.jar"]
