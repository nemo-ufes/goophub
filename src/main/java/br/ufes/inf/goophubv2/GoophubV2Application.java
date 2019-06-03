package br.ufes.inf.goophubv2;

import br.ufes.inf.goophubv2.Controller.UploadController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import java.io.File;

@SpringBootApplication
@ImportResource("classpath:applicationContext.xml")
public class GoophubV2Application {

	public static void main(String[] args) {

		new File(UploadController.uploadDirectory).mkdir();
		SpringApplication.run(GoophubV2Application.class, args);
	}

}
