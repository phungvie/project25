package viet.iot.project25.routes;


import lombok.AllArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import viet.iot.project25.entity.Order;
import viet.iot.project25.repository.OrderRepository;

@Component
@AllArgsConstructor
public class ComplexSedaRoute extends RouteBuilder {

    private OrderRepository orderRepository;

    @Override
    public void configure() {

        // Queue nhận yêu cầu từ API
        from("direct:orderInput")
                .to("seda:validateOrder");

        // Queue 1: Xác thực thông tin đơn hàng
        from("seda:validateOrder?concurrentConsumers=2")
                .log("Validating Order: ${body}")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    if (order.getCustomerName() == null || order.getProduct() == null) {
                        throw new IllegalArgumentException("Invalid Order Data");
                    }
                    order.setStatus("VALIDATED");
                    exchange.getIn().setBody(order);
                })
                .to("seda:transformOrder");

        // Queue 2: Chuyển đổi định dạng
        from("seda:transformOrder?concurrentConsumers=2")
                .log("Transforming Order: ${body}")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    order.setPrice(order.getPrice() * 1.1); // Thêm 10% thuế
                    order.setStatus("TRANSFORMED");
                    exchange.getIn().setBody(order);
                })
                .to("seda:callExternalAPI");

        // Queue 3: Gọi API bên ngoài
        from("seda:callExternalAPI?concurrentConsumers=2")
                .log("Calling External API for Order: ${body}")
                .to("https4://postman-echo.com/post")
                .log("External API Response: ${body}")
                .to("seda:saveOrder");

        // Queue 4: Lưu dữ liệu vào Database
        from("seda:saveOrder?concurrentConsumers=2")
                .log("Saving Order to Database: ${body}")
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    order.setStatus("COMPLETED");
                    orderRepository.save(order);
                })
                .log("Order Saved: ${body}");
    }
}
