// AppConfig.java
package com.ithra.library.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.io.FileInputStream;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableCaching
@Slf4j
public class AppConfig {

    @Value("${app.google.cloud.vision.credentials-path}")
    private String googleCredentialsPath;

    @Value("${app.processing.thread-pool-size}")
    private int threadPoolSize;

    @Value("${app.processing.queue-capacity}")
    private int queueCapacity;

    /**
     * Google Cloud Vision API Client
     */
    @Bean
    public ImageAnnotatorClient imageAnnotatorClient() throws Exception {
        log.info("Initializing Google Cloud Vision client");

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(googleCredentialsPath));

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        return ImageAnnotatorClient.create(settings);
    }

    /**
     * Thread pool for async processing
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("MediaProcessor-");
        executor.initialize();
        return executor;
    }

    /**
     * ModelMapper for DTO conversions
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
