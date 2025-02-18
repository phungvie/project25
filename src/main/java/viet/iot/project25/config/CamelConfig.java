/*package viet.iot.project25.config;

import org.apache.camel.CamelContext;
import org.apache.camel.Configuration;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.ThreadPoolProfile;
import org.springframework.context.annotation.Bean;

// Cấu hình Camel trong Spring Boot
@Configuration
public class CamelConfig {
    // Khởi tạo CamelContext làm Bean trong Spring
    @Bean
    public CamelContext camelContext() {
        DefaultCamelContext context = new DefaultCamelContext();
        configureThreadPool(context);
        return context;
    }

    // Cấu hình ThreadPoolProfile cho Camel
    private void configureThreadPool(CamelContext context) {
        ExecutorServiceManager manager = context.getExecutorServiceManager();
        ThreadPoolProfile threadPoolProfile = new ThreadPoolProfile();
        threadPoolProfile.setId("customThreadPool");
        threadPoolProfile.setPoolSize(10);
        threadPoolProfile.setMaxPoolSize(20);
        threadPoolProfile.setKeepAliveTime(60L);
        threadPoolProfile.setMaxQueueSize(100);
        manager.registerThreadPoolProfile(threadPoolProfile);
    }

    // Khởi tạo ProducerTemplate để gửi thông điệp trong Camel
    @Bean
    public ProducerTemplate producerTemplate(CamelContext camelContext) {
        return camelContext.createProducerTemplate();
    }

    // Khởi tạo FluentProducerTemplate để gửi thông điệp linh hoạt hơn
    @Bean
    public FluentProducerTemplate fluentProducerTemplate(CamelContext camelContext) {
        return camelContext.createFluentProducerTemplate();
    }
}*/
package viet.iot.project25.config;

import org.apache.camel.CamelContext;
import org.apache.camel.Configuration;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultStreamCachingStrategy;
import org.apache.camel.impl.health.ConsumersHealthCheckRepository;
import org.apache.camel.impl.health.ContextHealthCheck;
import org.apache.camel.impl.health.DefaultHealthCheckRegistry;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.ThreadPoolFactory;
import org.apache.camel.spi.ThreadPoolProfile;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.concurrent.CamelThreadFactory;
import org.springframework.context.annotation.Bean;
import viet.iot.project25.concurrent.ContainerExecutor;
import viet.iot.project25.concurrent.ContainerScheduledExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

// Cấu hình Camel trong Spring Boot
@Configuration
public class CamelConfig {
    // Khởi tạo CamelContext làm Bean trong Spring
    @Bean
    public CamelContext camelContext() {
        DefaultCamelContext context = new DefaultCamelContext();
        configureThreadPool(context);
        return context;
    }

    // Cấu hình ThreadPoolProfile cho Camel
    private void configureThreadPool(CamelContext context) {
        final ExecutorServiceManager executorServiceManager = context.getExecutorServiceManager();

// Not using InstrumentedThreadPoolFactory directly as it only uses the ThreadPoolProfile ID for naming
        ThreadPoolFactory threadPoolFactory = new ThreadPoolFactory() {
            private static final AtomicLong COUNTER = new AtomicLong();

            @Override
            public ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
                // This is an unlimited pool used probably only by multicast aggregation
                ExecutorService executorService = new ContainerExecutor(
                        getExecutorName("CachedPool", threadFactory),
                        1,
                        Integer.MAX_VALUE,
                        10,
                        -1,
                        new ThreadPoolExecutor.CallerRunsPolicy());

// Disabled as not very useful for SEDA components
//                if (meterRegistry != null) {
//                    executorService = ExecutorServiceMetrics.monitor(meterRegistry, executorService, name("instrumented-delegate-"));
//                }

                return executorService;
            }

            @Override
            public ExecutorService newThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {
                // This pool is used by SEDA consumers, so the endpoint parameters define the pool and queue sizes
                ExecutorService executorService = new ContainerExecutor(
                        getExecutorName("Pool", threadFactory),
                        profile.getPoolSize(),
                        profile.getMaxPoolSize(),
                        profile.getKeepAliveTime(),
                        profile.getMaxQueueSize(),
                        profile.getRejectedExecutionHandler()
                );

                return executorService;
            }

            @Override
            public ScheduledExecutorService newScheduledThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {
                ScheduledExecutorService scheduledExecutorService = new ContainerScheduledExecutor(
                        getExecutorName("ScheduledPool", threadFactory),
                        profile.getPoolSize(), profile.getRejectedExecutionHandler()
                );

// Disabled as not very useful for SEDA components
//                if (meterRegistry != null) {
//                    String name = getExecutorName("", threadFactory);
//                    name = "ScheduledPool".equals(name) ? profile.getId() : name;
//                    scheduledExecutorService = new TimedScheduledExecutorService(meterRegistry, scheduledExecutorService, name(name), Tags.empty());
//                }

                return scheduledExecutorService;
            }

            protected String getExecutorName(String name, ThreadFactory threadFactory) {
                if (threadFactory instanceof CamelThreadFactory factory) {
                    String camelName = factory.getName();
                    camelName = camelName.contains("://") ? StringHelper.after(camelName, "://") : camelName;
                    camelName = camelName.contains("?") ? StringHelper.before(camelName, "?") : camelName;
                    name = name + "-" + camelName;
                }
                return name;
            }

            private String name(String prefix) {
                return prefix + COUNTER.incrementAndGet();
            }
        };

        executorServiceManager.setThreadNamePattern("#counter# #name#");
        executorServiceManager.setThreadPoolFactory(threadPoolFactory);


        context.setAllowUseOriginalMessage(false);

        // Don't use JMS, we do our own correlation
        context.setUseBreadcrumb(false);

        // Enable health checks - Have to manually add built in ones for some reason
        context.setLoadHealthChecks(true);
        final var checkRegistry = new DefaultHealthCheckRegistry();
        checkRegistry.setExposureLevel("full");
        checkRegistry.register(new RoutesHealthCheckRepository());
        checkRegistry.register(new ConsumersHealthCheckRepository());
        checkRegistry.register(new ContextHealthCheck());
        context.getCamelContextExtension().addContextPlugin(HealthCheckRegistry.class, checkRegistry);

        // Force a quick shutdown of routes with in-flight exchanges
        context.getShutdownStrategy().setTimeout(5);
        context.getShutdownStrategy().setSuppressLoggingOnTimeout(true);

        context.setStreamCaching(true);
        StreamCachingStrategy streamCachingStrategy = new DefaultStreamCachingStrategy();
        streamCachingStrategy.setSpoolThreshold(524288); // 0.5MB
        context.setStreamCachingStrategy(streamCachingStrategy);

        context.getCamelContextExtension().setErrorHandlerFactory(new DefaultErrorHandlerBuilder());

//        if (container.isDevMode()) {
        context.setMessageHistory(true);
        context.setSourceLocationEnabled(true);
//        }



    }

    // Khởi tạo ProducerTemplate để gửi thông điệp trong Camel
    @Bean
    public ProducerTemplate producerTemplate(CamelContext camelContext) {
        return camelContext.createProducerTemplate();
    }

    // Khởi tạo FluentProducerTemplate để gửi thông điệp linh hoạt hơn
    @Bean
    public FluentProducerTemplate fluentProducerTemplate(CamelContext camelContext) {
        return camelContext.createFluentProducerTemplate();
    }
}