package com.example.camelspringboot;


import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MyRouteBuilder4 extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        var csvDf = new CsvDataFormat();
        csvDf.setLazyLoad("true");
        csvDf.setUseMaps("true");
        csvDf.setUseOrderedMaps("true");
        csvDf.setSkipHeaderRecord("true");


        from("file:input-cities-3?includeExt=csv&moveFailed=.failed&preMove=.inprogress")
                .routeId("input-cities-3")
                .streamCaching()
                .onException(Exception.class)
                    .maximumRedeliveries(3)
                    .onRedelivery(x -> {
                        log.warn("Redelivery in progess...");
                    })
                    .redeliveryDelay(30_000)
                    .logRetryAttempted(true)
                    .logContinued(true)
                    .logExhausted(true)
                .end()
                .log("Ricevuto file ${header.CamelFileName}")
                .to("direct:process-csv")
        ;


        from("direct:process-csv")
                .routeId("process-csv")
                .log("Prima di unmarshal")
                .unmarshal(csvDf)
                .log("Dopo unmarshal")
                .split(body()).streaming()
                    .log("Dentro lo split")
                    .setProperty("X-City-Name").body(b -> {
                        Map<String, String> bodyAsMap = (Map<String, String>) b;
                        return bodyAsMap.get("city");
                    }).setBody().body(b -> {
                        Map<String, String> bodyAsMap = (Map<String, String>) b;
                        return String.format("latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min&timezone=Europe/Berlin", bodyAsMap.get("lat"), bodyAsMap.get("lng"));
                    })
                    .to("direct:call-forecasts-api-4-pre")
                    .log("${exchangeProperty[X-City-Name]} -> ${body}")
                .end();


        from("direct:call-forecasts-api-4-pre")
                .routeId("call-forecasts-api-4-pre")
                .log("Dentro call-forecasts-api-4-pre: ${body}")
                .to("direct:call-forecasts-api-4");

        from("direct:call-forecasts-api-4")
                .onException(Exception.class)
                    .maximumRedeliveries(3)
                    .onRedelivery(x -> {
                        log.warn("Redelivery HTTP in progess...");
                    })
                    .redeliveryDelay(10_000)
                    .logRetryAttempted(true)
                    .logContinued(true)
                    .logExhausted(true)
                .end()
                .routeId("call-forecasts-api-4")
                .log("Dentro call-forecasts-api-4: ${body}")
                .setHeader(Exchange.HTTP_QUERY, body())
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .log("Prima della chiamata REST ${body}")
                .to("https://api.open-meteo.com/v1/forecasX").id("get-forecasts-4");


    }
}
