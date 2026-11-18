package com.crunchycode.cabbooking.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Safety checks for the deployment configuration. They read the files directly, so no Spring context
 * and no database are needed. If someone hardcodes a password or secret later, these tests fail.
 */
class ProductionConfigurationTest {

    private static Properties load(String classpathFile) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new ClassPathResource(classpathFile).getInputStream()) {
            properties.load(in);
        }
        return properties;
    }

    @Test
    void sensitiveSettings_comeFromEnvironmentVariables() throws IOException {
        Properties properties = load("application.properties");

        assertThat(properties.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET}"); // no default value at all
        assertThat(properties.getProperty("spring.datasource.password")).startsWith("${DB_PASSWORD");
        assertThat(properties.getProperty("spring.datasource.username")).startsWith("${DB_USERNAME");
        assertThat(properties.getProperty("app.admin.password")).startsWith("${ADMIN_PASSWORD");
    }

    @Test
    void noPasswordOrSecretIsHardcodedInAnyPropertiesFile() throws IOException {
        for (String file : List.of("application.properties", "application-prod.properties")) {
            Properties properties = load(file);
            for (String key : properties.stringPropertyNames()) {
                String lowerKey = key.toLowerCase();
                if (lowerKey.contains("password") || lowerKey.contains("secret")) {
                    assertThat(properties.getProperty(key))
                            .as("%s in %s must be an environment variable", key, file)
                            .startsWith("${");
                }
            }
        }
    }

    @Test
    void port_comesFromThePortVariable_andJwtExpiryIsConfigurable() throws IOException {
        Properties properties = load("application.properties");

        assertThat(properties.getProperty("server.port")).isEqualTo("${PORT:8080}");
        assertThat(properties.getProperty("app.jwt.expiration-minutes")).startsWith("${JWT_EXPIRATION_MINUTES");
    }

    @Test
    void schemaIsOwnedByFlyway_notByHibernate() throws IOException {
        Properties properties = load("application.properties");

        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void prodProfile_isQuietAndSafe() throws IOException {
        Properties prod = load("application-prod.properties");

        assertThat(prod.getProperty("spring.jpa.show-sql")).isEqualTo("false");
        assertThat(prod.getProperty("spring.jpa.properties.hibernate.generate_statistics")).isEqualTo("false");
        assertThat(prod.getProperty("server.error.include-stacktrace")).isEqualTo("never");
        assertThat(prod.getProperty("server.forward-headers-strategy")).isEqualTo("framework");
    }

    @Test
    void onlyTheHealthEndpointIsExposed() throws IOException {
        Properties properties = load("application.properties");

        assertThat(properties.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
        assertThat(properties.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
    }

    @Test
    void envExample_containsPlaceholdersOnly() throws IOException {
        List<String> lines = Files.readAllLines(Path.of(".env.example"));

        for (String name : List.of("DB_PASSWORD=", "JWT_SECRET=", "ADMIN_PASSWORD=")) {
            String line = lines.stream().filter(l -> l.startsWith(name)).findFirst().orElseThrow();
            assertThat(line).as("%s must be a <placeholder>", name).contains("<");
        }
    }

    @Test
    void realEnvFileIsIgnoredByGit() throws IOException {
        List<String> gitignore = Files.readAllLines(Path.of(".gitignore"));

        assertThat(gitignore).contains(".env");
    }

    @Test
    void dockerfile_usesProdProfile_andNoSecrets() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile).contains("SPRING_PROFILES_ACTIVE=prod");
        assertThat(dockerfile).contains("eclipse-temurin:21");
        assertThat(dockerfile.toLowerCase()).doesNotContain("password").doesNotContain("jwt_secret");
    }
}
