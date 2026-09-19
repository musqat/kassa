package com.kassa;

import org.springframework.boot.SpringApplication;

public class TestKassaApplication {

	public static void main(String[] args) {
		SpringApplication.from(KassaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
