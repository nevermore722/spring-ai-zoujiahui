package org.zjh.ai.springaizoujiahui.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean("boyfriendVectorStore")
    public VectorStore boyfriendVectorStore(EmbeddingModel embeddingModel) {
        JedisPooled jedisPooled = new JedisPooled(redisHost, redisPort);

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("boyfriend_knowledge")
                .prefix("bf:")
                .initializeSchema(true)
                .build();
    }

    @Bean("girlfriendVectorStore")
    public VectorStore girlfriendVectorStore(EmbeddingModel embeddingModel) {
        JedisPooled jedisPooled = new JedisPooled(redisHost, redisPort);

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("girlfriend_knowledge")
                .prefix("gf:")
                .initializeSchema(true)
                .build();
    }
}