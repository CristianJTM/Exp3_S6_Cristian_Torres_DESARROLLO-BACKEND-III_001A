package com.bancoxyz.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuditoriaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuditoriaServiceApplication.class, args);
	}
}