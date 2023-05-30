package com.example.camelspringboot;


import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MyRouteBuilder2 extends RouteBuilder {

    Predicate IS_A_BIG_CITY = PredicateBuilder.and(simple("${body[population]}").isNotNull(), PredicateBuilder.not(simple("${body[population]}").regex("^ *$")), simple("${body[population]}").convertTo(Double.class).isGreaterThan(30_000_000.0d));

    private final AtomicLong al = new AtomicLong(0);

    @Override
    public void configure() throws Exception {

        var csvDf = new CsvDataFormat();
        csvDf.setLazyLoad("true");
        csvDf.setUseMaps("true");
        csvDf.setUseOrderedMaps("true");
        csvDf.setSkipHeaderRecord("true");

        errorHandler(deadLetterChannel("direct:error-handler"));

        from("file:input-cities?includeExt=csv")
                .log("Ricevuto file ${header.CamelFileName}")
                .unmarshal(csvDf).split(body()).streaming()
                .filter(IS_A_BIG_CITY)
                .setProperty("X-City-Name").body(b -> {
                    Map<String, String> bodyAsMap = (Map<String, String>) b;
                    return bodyAsMap.get("city");
                }).setBody().body(b -> {
                    Map<String, String> bodyAsMap = (Map<String, String>) b;
                    return String.format("latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min&timezone=Europe/Berlin", bodyAsMap.get("lat"), bodyAsMap.get("lng"));
                }).log("${body}")
                    .to("direct:call-forecasts-api")
                    .filter(jsonpath(".daily.temperature_2m_max[0]").convertTo(Double.class).isGreaterThan(24.0))
                        .setHeader("X-Temperature").jsonpath(".daily.temperature_2m_max[0]")
                        .log("A ${exchangeProperty[X-City-Name]} ci sono ${header[X-Temperature]}")
                    .end()
                .end();

        from("direct:error-handler").log("${body} è andato in errore").stop();

        from("direct:call-forecasts-api")
                .routeId("call-forecasts-api")
                .setHeader(Exchange.HTTP_QUERY, body())
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("https://api.open-meteo.com/v1/forecast").id("get-forecasts");


    }
}
