# Vermity

Vermity is a simple and easy-to-use building management system. It leverages Spring Boot and Gradle to provide a robust backend service.

## Getting Started

### Prerequisites

- Java 11 or higher
- Gradle 6.8 or higher
- Docker and Docker Compose

### Installation

1. **Clone the repository:**
    ```sh
    git clone https://github.com/yourusername/vermity-backend.git
    cd vermity-backend
    ```

### Running with Docker Compose

1. **Build and start the services:**
    ```sh
    docker-compose up -d
    ```

2. **Access the application:**
    - Backend: `http://localhost:9913`

### Configuration

The application can be configured using environment variables defined in the `compose.yaml` file.

### Reference Documentation

For further reference, please consider the following sections:

- [Official Gradle documentation](https://docs.gradle.org)
- [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.3.0/gradle-plugin/reference/html/)
- [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.3.0/gradle-plugin/reference/html/#build-image)
- [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#using.devtools)
- [Spring Configuration Processor](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#appendix.configuration-metadata.annotation-processor)
- [Docker Compose Support](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#features.docker-compose)
- [Spring Web](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#web)
- [Spring Session](https://docs.spring.io/spring-session/reference/)
- [Spring Security](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#web.security)
- [Java Mail Sender](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#io.email)
- [Spring Data JPA](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#data.sql.jpa-and-spring-data)

### Guides

The following guides illustrate how to use some features concretely:

- [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
- [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
- [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
- [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
- [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
- [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

### Additional Links

These additional references should also help you:

- [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

### Docker Compose Support

This project contains a Docker Compose file named `compose.yaml`. In this file, the following services have been defined:

- mariadb: [`mariadb:latest`](https://hub.docker.com/_/mariadb)

Please review the tags of the used images and set them to the same as you're running in production.

