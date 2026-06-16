package com.jobfinder.config;

// Spring annotations and classes for injection, bean management, and configuration
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Spring Data Redis classes to handle connections and operations
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;

// standard java URI library to parse redis connection URLs
import java.net.URI;

// Marks this class as a configuration source for Redis-related beans
@Configuration
public class RedisConfig {

    // Injects the Redis connection URL defined in the application configuration properties
    @Value("${spring.data.redis.url}")
    private String redisUrl;

    // Defines a bean for the RedisConnectionFactory using Lettuce client
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Parses the redisUrl string into a URI object to extract connection parameters
        URI uri = URI.create(redisUrl);
        // Creates a standalone configuration object for Redis connection settings
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        // Extracts and sets the hostname from the Redis connection URI
        config.setHostName(uri.getHost());
        // Extracts the port number; defaults to 6379 if no port is specified (-1)
        config.setPort(uri.getPort() == -1 ? 6379 : uri.getPort());
        // Checks if user info (username:password) is present and not empty in the URI
        if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
            // Splits the user info into username and password parts by the colon delimiter
            String[] parts = uri.getUserInfo().split(":", 2);
            // If both username and password (or just password) are present, sets the password
            if (parts.length == 2) {
                config.setPassword(parts[1]);
            }
        }
        // Returns a Lettuce-based connection factory preconfigured with the Redis settings
        return new LettuceConnectionFactory(config);
    }

    /**
     * Generic string-keyed RedisTemplate used by DbWriteQueueService
     * for LPUSH/BRPOP queue operations and hash-based job metadata storage.
     */
    // Defines a bean for standard key-value Redis operations, using String keys and values
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        // Instantiates a new RedisTemplate
        RedisTemplate<String, String> template = new RedisTemplate<>();
        // Associates the Lettuce connection factory with this template
        template.setConnectionFactory(factory);
        // Instantiates a serializer for converting Java strings to UTF-8 bytes in Redis
        StringRedisSerializer serializer = new StringRedisSerializer();
        // Configures the template to use String serialization for standard keys
        template.setKeySerializer(serializer);
        // Configures the template to use String serialization for standard values
        template.setValueSerializer(serializer);
        // Configures the template to use String serialization for hash field keys
        template.setHashKeySerializer(serializer);
        // Configures the template to use String serialization for hash field values
        template.setHashValueSerializer(serializer);
        // Validates and finishes the internal configuration properties setup of the template
        template.afterPropertiesSet();
        // Returns the configured RedisTemplate bean
        return template;
    }
}
