package com.example.camelspringboot;


import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.FlexibleAggregationStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MyRouteBuilder extends RouteBuilder {


    private final AtomicLong al = new AtomicLong(0);

    @Override
    public void configure() throws Exception {

        var csvDf = new CsvDataFormat();
        csvDf.setLazyLoad("true");
        csvDf.setUseMaps("true");
        csvDf.setUseOrderedMaps("true");
        csvDf.setSkipHeaderRecord("true");

        from("file:input?includeExt=csv")
                .log("Ricevuto file ${header.CamelFileName}")
                .multicast(AggregationStrategies.useOriginal(), true)
                    .to(ExchangePattern.InOut, "direct:elaborazione-b")
                    .to(ExchangePattern.InOut,"direct:elaborazione-a")
                .end()
                .log("Ho terminato entrambe le elaborazioni")

        ;

        from("direct:elaborazione-b")
                .unmarshal(csvDf)
                .split(body()).streaming()
                .aggregate(simple("${body[DESTINATION_AIRPORT]}"), AggregationStrategies.groupedBody())
                .completionTimeout(3_000)
                .log("Gruppo ${exchangeProperty.CamelAggregatedCorrelationKey} con ${body.size} elementi");

        from("direct:elaborazione-a")
                .unmarshal(csvDf)
                .split(body(), (oldExchange, newExchange) -> {
                    if (oldExchange == null) {
                        String airport = newExchange.getMessage().getBody(String.class);
                        HashMap<String, Long> hm = new HashMap<>();
                        hm.put(airport, 1L);
                        newExchange.getMessage().setBody(hm);
                        return newExchange;
                    }
                    Map<String, Long> hm = oldExchange.getMessage().getBody(Map.class);
                    String airport = newExchange.getMessage().getBody(String.class);
                    hm.compute(airport, (k, v) -> v == null ? 1L : v + 1L);
                    return oldExchange;
                }).streaming()
                .to("direct:line")
                .end()
                .log("Ci sono in tutto ${body.size()} aereoporti: ${body}");


        from("direct:line")
                .setBody().body(b -> ((Map) b).get("DESTINATION_AIRPORT"))
        //.log("-> ${body}");
        ;
    }
}
