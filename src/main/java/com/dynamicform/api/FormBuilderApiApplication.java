package com.dynamicform.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FormBuilderApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FormBuilderApiApplication.class, args);
	}

}
