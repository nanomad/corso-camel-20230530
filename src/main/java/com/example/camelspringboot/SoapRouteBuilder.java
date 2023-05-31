package com.example.camelspringboot;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.springframework.stereotype.Component;

@Component
public class SoapRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        //SoapDataFormat soapDF = new SoapDataFormat("org.tempuri", new ServiceInterfaceStrategy(CalculatorSoap.class, true));
        from("timer:soap-client?repeatCount=50&period=15000")
                .setBody().constant(new Object[]{1, 2})
                .setHeader(CxfConstants.OPERATION_NAME, constant("Add"))
                .to("cxf://http://www.dneonline.com/calculator.asmx?serviceClass=org.tempuri.CalculatorSoap")
                .process(x -> {
                    System.out.println(x);
                });
    }
}
