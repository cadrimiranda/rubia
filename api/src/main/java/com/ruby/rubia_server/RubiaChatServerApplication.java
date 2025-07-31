package com.ruby.rubia_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableScheduling
@EnableAsync
@Slf4j
public class RubiaChatServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RubiaChatServerApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		log.info("🚀 === RUBIA CHAT SERVER STARTED ===");
		log.info("🚀 Server is ready to receive requests");
		log.info("🚀 Z-API webhook endpoint: /api/messaging/webhook/zapi");
		log.info("🚀 Test endpoint: /api/messaging/webhook/zapi/test");
		log.info("🚀 === DEBUG MODE ENABLED ===");
	}
}
