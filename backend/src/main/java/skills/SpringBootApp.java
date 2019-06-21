package skills;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import java.util.TimeZone;

@EnableScheduling
@EnableWebSecurity
@SpringBootApplication
@EnableJpaRepositories(basePackages = {"skills.storage.repos"})
@ImportResource(locations = {"classpath:app-context-db.xml"})
public class SpringBootApp {

    static final String DISABLE_HOSTNAME_VERIFIER_PROP = "skills.disableHostnameVerifier";

    public static void main(String[] args) {
        // must call in the main method and not in @PostConstruct method as H2 jdbc driver will cache timezone prior @PostConstruct method is called
        // alternatively we could pass in -Duser.timezone=UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        boolean disableHostnameVerifier = Boolean.parseBoolean(System.getProperty(DISABLE_HOSTNAME_VERIFIER_PROP));
        if (disableHostnameVerifier) {
            HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);
        }

        SpringApplication.run(SpringBootApp.class, args);
    }
}
