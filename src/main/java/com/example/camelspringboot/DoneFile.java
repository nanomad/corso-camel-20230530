package com.example.camelspringboot;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class DoneFile extends RouteBuilder {

    private static class MyAggrStratgy implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            System.out.println("old" + oldExchange);
            System.out.println("new" + newExchange);
            if (newExchange != null) {
                return newExchange;
            }
            throw new IllegalStateException("File non arrivato in tempo");
        }
    }
    @Override
    public void configure() throws Exception {



        from("file:input-copy-2?includeExt=done&delete=true")
                .routeId("copy-file-trigger-new")
                .log("File di avvio ${header.CamelFileName} arrivato")
                .pollEnrich()
                    .simple("file:input-copy-2?includeExt=csv&fileName=${header.CamelFilenameOnly}.csv&move=../output-copy")
                    .aggregationStrategy(new MyAggrStratgy())
                    .timeout(5_000)
                    .log("File di ingresso arrivato: ${header.CamelFilenameOnly}")
                .end();



    }
}
