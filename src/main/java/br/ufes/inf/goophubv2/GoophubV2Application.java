package br.ufes.inf.goophubv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:applicationContext.xml")
public class GoophubV2Application {

	public static void main(String[] args) {
		SpringApplication.run(GoophubV2Application.class, args);
	}

}
