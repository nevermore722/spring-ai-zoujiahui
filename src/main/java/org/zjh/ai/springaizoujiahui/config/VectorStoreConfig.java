package org.zjh.ai.springaizoujiahui.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class VectorStoreConfig {

    @Autowired
    private EmbeddingModel embeddingModel;  // Spring AI 自动注入

    @Bean
    public VectorStore vectorStore() {
        // 直接连接 Redis
        JedisPooled jedisPooled = new JedisPooled("localhost", 6379);

        // 创建 RedisVectorStore
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("my-knowledge")     // 索引名称
                .prefix("doc:")                // key 前缀
                .initializeSchema(true)        // 自动创建索引
                .build();
    }
}