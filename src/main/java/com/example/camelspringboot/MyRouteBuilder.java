package com.example.camelspringboot;


import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MyRouteBuilder extends RouteBuilder {

    private static final String MY_FILENAME_PROP = "X-Original-Filename";

    static class MyBean {
        public String setToUpperCase(String message) {
            return message.toUpperCase(Locale.ENGLISH);
        }
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class).maximumRedeliveries(1);

        getCamelContext().setTracing(true);


        from("file:input") // Exchange creata qui
                .routeId("read-input-folder")
                .to("seda:process-file?size=10&blockWhenFull=true");

        from("seda:process-file?concurrentConsumers=10")
                .routeId("process-input-file-parallel")
                .log(LoggingLevel.INFO, "Ricevuto un nuovo file: ${header.CamelFileNameOnly}")
                .process(exchange -> {
                    var body = exchange.getMessage().getBody(String.class);
                    String[] tokens = body.split(";", -1);
                    String stringa = String.format("latitude=%s&longitude=%s", tokens[0], tokens[1]);
                    exchange.getMessage().setBody(stringa);
                })
                .setProperty(MY_FILENAME_PROP, header(Exchange.FILE_NAME_ONLY))
                .to("direct:call-forecasts-api")
                .to("direct:write-output-file");


        from("direct:call-forecasts-api")
                .routeId("call-forecasts-api")
                .setHeader(Exchange.HTTP_QUERY, simple("${body}"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("https://api.open-meteo.com/v1/forecast").id("get-forecasts")
                .unmarshal().json(JsonLibrary.Jackson);

        from("direct:write-output-file")
                .routeId("write-output-file")
                .marshal().csv()
                .log("${body}")
                .log("File ${header.CamelFileNameOnly} spostato correttamente")
                .toD("file:output?fileExist=Override&fileName=${exchangeProperty.X-Original-Filename}").id("to-write-output-file");
        // Exchange distrutta qui
    }
}
