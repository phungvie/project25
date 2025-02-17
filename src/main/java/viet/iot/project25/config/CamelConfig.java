package viet.iot.project25.config;

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
}