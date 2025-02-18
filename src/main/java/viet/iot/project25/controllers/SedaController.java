package viet.iot.project25.controllers;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seda")
public class SedaController {

    @Autowired
    private ProducerTemplate producerTemplate;

    @PostMapping("/send")
    public String sendMessage(@RequestBody String message) {
        producerTemplate.sendBody("direct:start", message);
        return "Message sent: " + message;
    }
}
