package viet.iot.project25.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

// Dịch vụ MessageBrokerService để quản lý CamelContext
@Component
@AllArgsConstructor
@Getter
@Setter
public class MessageBrokerService {

    private final CamelContext context;
    private final ProducerTemplate producerTemplate;
    private final FluentProducerTemplate fluentProducerTemplate;

    // Tự động khởi động CamelContext khi Bean được tạo
    @PostConstruct
    public void start() throws Exception {
        context.start();
    }

    // Tự động dừng CamelContext khi Bean bị hủy
    @PreDestroy
    public void stop() throws Exception {
        context.stop();
    }
}
