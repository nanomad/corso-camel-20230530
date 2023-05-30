package com.example.camelspringboot;


import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.FlexibleAggregationStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.ILoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MyRouteBuilder extends RouteBuilder {

    private static class AggregateToListStrategy implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange currentOutput, Exchange currentInput) {
            if (currentOutput == null) {
                String body = currentInput.getMessage().getBody(String.class);
                ArrayList<Object> aggregatedBody = new ArrayList<>();
                aggregatedBody.add(body);
                currentInput.getMessage().setBody(aggregatedBody);
                return currentInput;
            }
            List aggregatedBody = currentOutput.getMessage().getBody(List.class);
            String body = currentInput.getMessage().getBody(String.class);
            aggregatedBody.add(body);
            currentOutput.getMessage().setBody(aggregatedBody);
            return currentOutput;
        }
    }


    @Override
    public void configure() throws Exception {
        from("timer:my-schedule?delay=350")
                .routeId("generate-messages")
                .process(exchange -> {
                    exchange.getMessage().setBody(UUID.randomUUID().toString());
                    exchange.getMessage().setHeader("X-Batch-Id", ((int) (Math.random() * 5)));
                })
                .log("Messaggio generato: ${body} ${header.X-Batch-Id}")
                .filter(exchange -> exchange.getMessage().getBody(String.class).startsWith("9"))
                .log("Messaggio sopravvissuto: ${body} ${header.X-Batch-Id}")
                .aggregate(header("X-Batch-Id"), new FlexibleAggregationStrategy<String>()
                        //.condition(exchange -> exchange.getMessage().getBody(String.class).startsWith("9"))
                        .pick(body())
                        .castAs(String.class)
                        .accumulateInCollection(ArrayList.class)
                        .storeInBody()
                )
                .completionInterval(30000)
                .log("Dopo l'aggregazione ho ${body} ${header.X-Batch-Id}");
    }
}
