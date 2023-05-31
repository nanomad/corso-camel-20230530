package com.example.camelspringboot;

import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Component
public class StoreCitiesSQL extends RouteBuilder {

    private final PlatformTransactionManager transactionManager;
    private final MyService myService;

    public StoreCitiesSQL(PlatformTransactionManager transactionManager, MyService myService) {
        this.transactionManager = transactionManager;
        this.myService = myService;
    }

    @Override
    public void configure() throws Exception {

        var csvDf = new CsvDataFormat();
        csvDf.setLazyLoad("true");
        csvDf.setUseMaps("true");
        csvDf.setUseOrderedMaps("true");
        csvDf.setSkipHeaderRecord("true");


        from("file:input-cities-4?moveFailed=.failed")
                .routeId("store-cities-via-jdbc")
                    .onException(Exception.class)
                    .maximumRedeliveries(0)
                    .log("FAILED")
                .end()
                .setHeader("X-Test", constant("TEST"))
                .setBody().constant(
                        Map.of(
                                "city", "Tokyo3",
                                "iso2", "JP",
                                "iso3", "JPN"
                        )
                )
                .to("sql:classpath:insert-query.sql?allowNamedParameters=true")
                .doTry()
                    .process(myService)
                    .to("direct:this-is-transactional")
                .doFinally()
                    .log("${header.X-Test}")
                .end()
        ;

        from("direct:this-is-transactional")
                .routeId("this-is-transactional")
                .transacted()
                .setHeader("X-Test").message(m -> m.getHeader("X-Test", String.class) + "!")
                .setBody().constant(
                        Map.of(
                                "city", "Tokyo",
                                "iso2", "JP",
                                "iso3", "JPN"
                        )
                )
                .to("sql:classpath:insert-query.sql?allowNamedParameters=true")
                .setBody().constant(
                        Map.of(
                                "city", "Tokyo2",
                                "iso2", "JP",
                                "iso3", "JPN"
                        )
                )
                .to("sql:classpath:insert-query.sql?allowNamedParameters=true")
        ;

        from("sql:SELECT * FROM CITIES?outputType=StreamList&delay=5000&repeatCount=50&maxMessagesPerPoll=1")
                .process(x -> {
                    log.warn("{}", x.getMessage().getBody().getClass());
                })
                .split(body()).streaming()
                .log("${body}")
                .to("sql:classpath:delete-city.sql")
                .end()
        ;

    }
}
