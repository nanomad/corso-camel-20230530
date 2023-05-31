package com.example.camelspringboot;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ControlBusDoneFile extends RouteBuilder {
    @Override
    public void configure() throws Exception {


        from("file:input-copy?includeExt=done&delete=true")
                .routeId("copy-file-trigger")
                .log("File di avvio ${header.CamelFileName} arrivato")
                .to("controlbus:route?action=start&routeId=copy-file");


        from("file:input-copy?includeExt=csv&move=../output-copy")
                .routeId("copy-file")
                .noAutoStartup()
                .log("File di ingresso arrivato")
                .to(ExchangePattern.InOnly, "controlbus:route?action=stop&routeId=copy-file&async=true");
    }
}
