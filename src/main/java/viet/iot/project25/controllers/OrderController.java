package viet.iot.project25.controllers;


import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import viet.iot.project25.entity.Order;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private ProducerTemplate producerTemplate;

    @PostMapping
    public String createOrder(@RequestBody Order order) {
        producerTemplate.sendBody("direct:orderInput", order);
        return "Order is being processed!";
    }
}
