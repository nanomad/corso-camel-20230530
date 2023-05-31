package com.example.camelspringboot;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CallThisFromRest extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:insert-tokio")
                .routeId("insert-tokio")
                .transacted()
                .setHeader("X-Test").message(m -> m.getHeader("X-Test", String.class) + "!")
                .setHeader("X-Test2").body(String.class)
                .log("Questo è il body che ho ricevuto da Spring: ${body}")
                .to("sql:classpath:insert-query.sql?allowNamedParameters=true")
                .to("log:Stuff?showAll=true")
        ;
    }
}
