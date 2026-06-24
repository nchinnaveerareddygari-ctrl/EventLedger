package com.eventledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

@SpringBootApplication
public class EventGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventGatewayApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public Tracer tracer() {
		SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
				.addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
				.build();
		OpenTelemetrySdk.builder()
				.setTracerProvider(tracerProvider)
				.buildAndRegisterGlobal();
		return tracerProvider.tracerBuilder("event-gateway-tracer").build();
	}
}
