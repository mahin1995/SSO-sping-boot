package com.example.sso.serviceb;

import com.example.sso.serviceb.config.EnvFileBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServiceBApplication {

	public static void main(String[] args) {
		EnvFileBootstrap.loadForServiceB();
		SpringApplication.run(ServiceBApplication.class, args);
	}

}
