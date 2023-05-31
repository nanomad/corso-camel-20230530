package com.example.camelspringboot;


import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MyRouteBuilder3 extends RouteBuilder {

    private final AtomicLong al = new AtomicLong(0);

    @Override
    public void configure() throws Exception {

        var csvDf = new CsvDataFormat();
        csvDf.setLazyLoad("true");
        csvDf.setUseMaps("true");
        csvDf.setUseOrderedMaps("true");
        csvDf.setSkipHeaderRecord("true");


        from("file:input-cities-2?includeExt=csv")
                .errorHandler(deadLetterChannel("direct:error-handler2"))
                .log("Ricevuto file ${header.CamelFileName}")
                .unmarshal(csvDf)
                .split(body()).streaming()
                .setProperty("X-Destination").body(b -> {
                    Map<String, String> bodyAsMap = (Map<String, String>) b;
                    var country = bodyAsMap.get("country").toLowerCase();
                    var iso2 = bodyAsMap.get("iso2").toLowerCase();

                    return "direct:country-" + country + "|" + "direct:iso2-" + iso2;
                })
                .setProperty("X-City-Name").body(b -> {
                    Map<String, String> bodyAsMap = (Map<String, String>) b;
                    return bodyAsMap.get("city");
                }).setBody().body(b -> {
                    Map<String, String> bodyAsMap = (Map<String, String>) b;
                    return String.format("latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min&timezone=Europe/Berlin", bodyAsMap.get("lat"), bodyAsMap.get("lng"));
                })
                .log("${body}")
                .to("direct:call-forecasts-api")
                .recipientList(exchangeProperty("X-Destination"), "|")
                .end()
                .end();

        from("direct:country-indonesia").log("Indonesia");
        from("direct:iso2-in").log("IN");
        from("direct:country-japan").log("Japan");
        from("direct:iso2-jp").log("JP");
        from("direct:country-india").log("India");
        from("direct:iso2-id").log("ID");

        from("direct:error-handler2").log("${body} è andato in errore").stop();

        from("direct:call-forecasts-api")
                .routeId("call-forecasts-api")
                .setHeader(Exchange.HTTP_QUERY, body())
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("https://api.open-meteo.com/v1/forecast").id("get-forecasts");


    }
}
