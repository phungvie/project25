package viet.iot.project25.routes;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class SedaRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Đẩy thông điệp vào hàng đợi SEDA
        from("direct:start")
                .to("seda:processQueue");

        // Xử lý thông điệp từ hàng đợi SEDA
        from("seda:processQueue?concurrentConsumers=2")
                .log("Received Message: ${body}")
                .process(exchange -> {
                    String message = exchange.getIn().getBody(String.class);
                    exchange.getIn().setBody("Processed: " + message);
                })
                .to("seda:resultQueue");

        // Lấy kết quả từ hàng đợi SEDA
        from("seda:resultQueue")
                .log("Final Output: ${body}");
    }
}
