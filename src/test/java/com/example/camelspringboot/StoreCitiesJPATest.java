package com.example.camelspringboot;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpointsAndSkip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@SpringBootTest
@MockEndpointsAndSkip("jpa:*")
class StoreCitiesJPATest {

    @EndpointInject("file:input-cities-4")
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:jpa:City")
    MockEndpoint mockEndpoint;

    @Autowired
    CamelContext camelContext;

    @Test
    public void shouldCallJpa() throws Exception {
        mockEndpoint.setExpectedMessageCount(1);

        producerTemplate.sendBodyAndHeaders(
                getClass().getResourceAsStream("/cities.small.csv"),
                Map.of(
                        Exchange.FILE_NAME, "mock.csv",
                        Exchange.FILE_NAME_ONLY, "mock.csv"
                )
        );


        Object result = producerTemplate.requestBody(Map.of());


        MockEndpoint.assertIsSatisfied(camelContext);

    }
}