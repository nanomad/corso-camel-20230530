package com.example.camelspringboot;

import com.example.camelspringboot.entity.City;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Component
public class StoreCitiesJPA extends RouteBuilder {


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
                .transacted()
                .unmarshal(csvDf)
                .split().body().streaming().aggregationStrategy(AggregationStrategies.useOriginal())
                .setBody().body(b -> {
                    Map bodyAsMap = (Map) b;
                    City city = new City();
                    city.setCity(((String) bodyAsMap.get("city")));
                    city.setIso2(((String) bodyAsMap.get("iso2")));
                    city.setIso3(((String) bodyAsMap.get("iso3")));
                    return city;
                })
                .to("jpa:City")
                .log("Stored ${body}")
                .end();

        from("jpa:City?namedQuery=findTokio&delay=3000&repeatCount=50")
                .split(body()).streaming()
                .log("Found ${body}")
                .end()


        ;


    }
}
