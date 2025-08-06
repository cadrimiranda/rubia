package com.ruby.rubia_server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestRedisContainerConfiguration {

    private static volatile GenericContainer<?> redisContainer;
    
    public static GenericContainer<?> getRedisContainer() {
        if (redisContainer == null) {
            synchronized (TestRedisContainerConfiguration.class) {
                if (redisContainer == null) {
                    redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
                            .withExposedPorts(6379)
                            .withReuse(true);
                    redisContainer.start();
                }
            }
        }
        return redisContainer;
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        GenericContainer<?> redis = getRedisContainer();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            redis.getHost(), 
            redis.getMappedPort(6379)
        );
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serialization for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use Generic serialization for values
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
        
        template.afterPropertiesSet();
        return template;
    }
}