package com.example.camelspringboot;

import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jdbc.JdbcConstants;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

public class StoreCities extends RouteBuilder {

    private final PlatformTransactionManager transactionManager;

    public StoreCities(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
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
                .transacted().policy(new SpringTransactionPolicy(transactionManager))
                .setHeader(JdbcConstants.JDBC_PARAMETERS).constant(
                        Map.of(
                                "city", "Tokyo",
                                "iso2", "JP",
                                "iso3", "JPN"
                        )
                )
                .setBody(constant("INSERT INTO CITIES(city, iso2, iso3) VALUES ( :?city , :?iso2 , :?iso3 )"))
                .to("spring-jdbc:dataSource?allowNamedParameters=true&useHeadersAsParameters=true&transacted=true")
                .setHeader(JdbcConstants.JDBC_PARAMETERS).constant(
                        Map.of(
                                "city", "Tokyo2",
                                "iso2", "JP",
                                "iso3", "JPN"
                        )
                ).setBody(constant("INSERT INTO CITIES(city, iso2, iso3) VALUES ( :?city , :?iso2 , :?iso3 )"))
                .to("spring-jdbc:dataSource?allowNamedParameters=true&useHeadersAsParameters=true&transacted=true")
        ;

        from("timer:check-cities?repeatCount=50&period=3000&fixedRate=false")
                .setBody(constant("SELECT * FROM CITIES"))
                .enrich("spring-jdbc:dataSource?outputType=StreamList", AggregationStrategies.useLatest())
                .process(x -> {
                    log.warn("{}", x.getMessage().getBody().getClass());
                })
                .split(body()).streaming()
                .log("${body}")
                .end()
        ;

    }
}
