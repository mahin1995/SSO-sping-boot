package com.example.sso.servicea;

import com.example.sso.servicea.config.EnvFileBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServiceAApplication {

	public static void main(String[] args) {
		EnvFileBootstrap.loadForServiceA();
		SpringApplication.run(ServiceAApplication.class, args);
	}

}
