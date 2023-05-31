package com.example.camelspringboot;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stuff")
public class MyRestController {

    @Autowired
    @EndpointInject("direct:insert-tokio")
    ProducerTemplate producerTemplate;
    @GetMapping()
    public Map<String, String> doStuff(@RequestParam("city") String city, @RequestParam("iso2") String iso2, @RequestParam("iso3") String iso3) {
        Map<String, String> camelRequestBody = Map.of("city", city, "iso2", iso2, "iso3", iso3);
        Object result = producerTemplate.requestBodyAndHeader(camelRequestBody, "X-Test", "Value di X-Test");
        return ((Map) result);
    }
}
