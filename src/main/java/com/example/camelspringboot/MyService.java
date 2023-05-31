package com.example.camelspringboot;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service("myService")
public class MyService implements Processor {

    @Transactional
    public void process(Exchange exchange) {
        System.err.println("OK");
    }
}
