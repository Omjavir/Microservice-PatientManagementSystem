package com.example.billingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BillingServiceApplication {

    public static void main(String[] args) {
        System.out.println("Hello Billing Service");
        SpringApplication.run(BillingServiceApplication.class, args);
    }

}
