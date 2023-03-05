package org.springframework.data.cockroachdb.basics;

import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.CockroachDialect;
import org.postgresql.PGProperty;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.cockroachdb.aspect.AdvisorOrder;
import org.springframework.data.cockroachdb.aspect.TransactionAttributesAspect;
import org.springframework.data.cockroachdb.aspect.TransactionRetryAspect;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

import io.cockroachdb.jdbc.CockroachProperty;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

// Bump up to enable extra advisors with lower priority
@EnableTransactionManagement(order = AdvisorOrder.TRANSACTION_ADVISOR)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class
})
@EnableJpaRepositories(basePackageClasses = JpaBasicsConfiguration.class)
@ComponentScan(basePackageClasses = JpaBasicsConfiguration.class)
@Configuration
public class JpaBasicsConfiguration {
    @Bean
    public TransactionRetryAspect transactionRetryAspect() {
        return new TransactionRetryAspect();
    }

    @Bean
    public TransactionAttributesAspect transactionBoundaryAspect(JdbcTemplate jdbcTemplate) {
        return new TransactionAttributesAspect(jdbcTemplate);
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource() {
        HikariDataSource ds = dataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
        ds.addDataSourceProperty(PGProperty.REWRITE_BATCHED_INSERTS.getName(), "true");

        ds.addDataSourceProperty(CockroachProperty.IMPLICIT_SELECT_FOR_UPDATE.getName(), "true");
        ds.addDataSourceProperty(CockroachProperty.RETRY_TRANSIENT_ERRORS.getName(), "true");
        ds.addDataSourceProperty(CockroachProperty.RETRY_MAX_ATTEMPTS.getName(), "10");
        ds.addDataSourceProperty(CockroachProperty.RETRY_MAX_BACKOFF_TIME.getName(), "15000");

        return ProxyDataSourceBuilder
                .create(ds)
                .name("SQL-Trace")
                .asJson()
                .logQueryBySlf4j(SLF4JLogLevel.TRACE, "SQL_TRACE")
                .build();
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager(emf);
        transactionManager.setJpaDialect(new HibernateJpaDialect());
        return transactionManager;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource());
        emf.setJpaProperties(jpaVendorProperties());
        emf.setJpaVendorAdapter(jpaVendorAdapter());
        emf.setPackagesToScan("org.springframework.data.cockroachdb.basics");
        return emf;
    }

    private Properties jpaVendorProperties() {
        return new Properties() {
            {
                setProperty(Environment.USE_MINIMAL_PUTS, Boolean.TRUE.toString());
                setProperty(Environment.CACHE_REGION_FACTORY, NoCachingRegionFactory.class.getName());
                setProperty(Environment.STATEMENT_BATCH_SIZE, "64");
                setProperty(Environment.ORDER_INSERTS, "true");
                setProperty(Environment.ORDER_UPDATES, "true");
                setProperty(Environment.BATCH_VERSIONED_DATA, "true");

                setProperty(Environment.GENERATE_STATISTICS, Boolean.TRUE.toString());
                setProperty(Environment.LOG_SESSION_METRICS, Boolean.FALSE.toString());
                setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
            }
        };
    }

    private JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabasePlatform(CockroachDialect.class.getName());
        vendorAdapter.setDatabase(Database.POSTGRESQL);
        return vendorAdapter;
    }
}
