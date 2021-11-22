package com.pdf.demo;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
public class PdfDemoApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(PdfDemoApplication.class, args);
		
	}

}
