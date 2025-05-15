package com.egov.contractservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class ContractServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(ContractServiceApplication.class, args);
    }

}
