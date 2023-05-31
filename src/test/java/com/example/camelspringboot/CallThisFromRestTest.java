package com.example.camelspringboot;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.apache.camel.test.spring.junit5.MockEndpointsAndSkip;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest
@MockEndpointsAndSkip("sql:*")
class CallThisFromRestTest {

    @EndpointInject("direct:insert-tokio")
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:sql:classpath:insert-query.sql")
    MockEndpoint mockEndpoint;

    @Autowired
    CamelContext camelContext;

    @Test
    public void doStuff() throws Exception {
        mockEndpoint.setExpectedMessageCount(1);
        mockEndpoint.whenAnyExchangeReceived(x -> {
            x.getMessage().setBody("CIAO");
        });

        Object result = producerTemplate.requestBody(Map.of());


        MockEndpoint.assertIsSatisfied(camelContext);

        Assertions.assertEquals(Map.of(), mockEndpoint.getExchanges().get(0).getMessage().getBody());

        Assertions.assertEquals("CIAO", result);
    }

}