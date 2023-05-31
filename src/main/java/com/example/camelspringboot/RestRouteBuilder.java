package com.example.camelspringboot;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class RestRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("rest:get:hello/{me}")
            .to("direct:hello-2");


        rest()
            .get("/hello2/{me}")
            .to("direct:hello-2")

        ;

        from("direct:hello-2")
                .transform().simple("Bye ${header.me}");

    }
}
