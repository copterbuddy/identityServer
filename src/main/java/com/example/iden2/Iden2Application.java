package com.example.iden2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Iden2Application {

	public static void main(String[] args) {
		SpringApplication.run(Iden2Application.class, args);
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cennectionFactory) {
		var template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(cennectionFactory);
		return template;
	}
}
