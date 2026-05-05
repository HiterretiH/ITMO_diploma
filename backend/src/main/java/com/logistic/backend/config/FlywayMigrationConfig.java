package com.logistic.backend.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayMigrationConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    static BeanFactoryPostProcessor flywayRunsBeforeJpa() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                for (String name :
                        beanFactory.getBeanNamesForType(LocalContainerEntityManagerFactoryBean.class, true, false)) {
                    String defName =
                            name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)
                                    ? name.substring(BeanFactory.FACTORY_BEAN_PREFIX.length())
                                    : name;
                    if (!beanFactory.containsBeanDefinition(defName)) {
                        continue;
                    }
                    var bd = beanFactory.getBeanDefinition(defName);
                    String[] deps = bd.getDependsOn();
                    if (deps != null && Arrays.asList(deps).contains("flywayMigrationRunner")) {
                        continue;
                    }
                    String[] next = new String[(deps == null ? 0 : deps.length) + 1];
                    if (deps != null) {
                        System.arraycopy(deps, 0, next, 0, deps.length);
                    }
                    next[next.length - 1] = "flywayMigrationRunner";
                    bd.setDependsOn(next);
                }
            }
        };
    }

    @Bean
    FlywayMigrationRunner flywayMigrationRunner(
            DataSource dataSource,
            @org.springframework.beans.factory.annotation.Value("${spring.flyway.locations:classpath:db/migration}")
                    String locations) {
        return new FlywayMigrationRunner(dataSource, locations);
    }

    static final class FlywayMigrationRunner {

        private final DataSource dataSource;
        private final String locations;

        FlywayMigrationRunner(DataSource dataSource, String locations) {
            this.dataSource = dataSource;
            this.locations = locations;
        }

        @PostConstruct
        void migrate() {
            String[] locs =
                    Arrays.stream(locations.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
            Flyway.configure().dataSource(dataSource).locations(locs).load().migrate();
        }
    }
}
