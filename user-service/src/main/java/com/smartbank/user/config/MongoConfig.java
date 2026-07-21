package com.smartbank.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables MongoDB auditing  */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
