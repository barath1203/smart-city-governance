package com.smartcity.governance;

import org.springframework.boot.SpringApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartCityGovernanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartCityGovernanceApplication.class, args);
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("1234"));
	}

}
