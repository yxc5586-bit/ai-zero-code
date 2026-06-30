package com.cyx.aizerocode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class AiZeroCodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiZeroCodeApplication.class, args);
	}

}
