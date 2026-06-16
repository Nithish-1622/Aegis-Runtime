package com.aegis.runtime.config.flyway;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

@Configuration(proxyBeanMethods = false)
public class FlywayMigrationConfig {

    @Bean
    public static BeanFactoryPostProcessor flywayDependsOnEntityManagerFactoryPostProcessor() {
        return new EntityManagerFactoryDependsOnFlywayPostProcessor();
    }

    @Bean(name = "flywayMigrationBean")
    public FlywayMigrationBean flywayMigrationBean(DataSource dataSource) {
        return new FlywayMigrationBean(dataSource);
    }

    public static final class FlywayMigrationBean {

        private final DataSource dataSource;

        public FlywayMigrationBean(DataSource dataSource) {
            this.dataSource = dataSource;
            migrate();
        }

        private void migrate() {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
        }
    }

    public static final class EntityManagerFactoryDependsOnFlywayPostProcessor
            implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            if (registry.containsBeanDefinition("entityManagerFactory")) {
                RootBeanDefinition definition = (RootBeanDefinition) registry.getBeanDefinition("entityManagerFactory");
                definition.setDependsOn("flywayMigrationBean");
            }
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            // no-op
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
