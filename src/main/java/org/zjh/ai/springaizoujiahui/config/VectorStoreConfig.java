package org.zjh.ai.springaizoujiahui.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class VectorStoreConfig {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    /**
     * 男朋友专用向量库（技术、编程、游戏等知识）
     */
    @Bean("boyfriendVectorStore")
    public VectorStore boyfriendVectorStore(EmbeddingModel embeddingModel) {
        JedisPooled jedisPooled = new JedisPooled(REDIS_HOST, REDIS_PORT);

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("boyfriend_knowledge")     // 男朋友专用索引
                .prefix("bf:")                         // bf 前缀
                .initializeSchema(true)
                .build();
    }

    /**
     * 女朋友专用向量库（美妆、穿搭、生活、情感等知识）
     */
    @Bean("girlfriendVectorStore")
    public VectorStore girlfriendVectorStore(EmbeddingModel embeddingModel) {
        JedisPooled jedisPooled = new JedisPooled(REDIS_HOST, REDIS_PORT);

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("girlfriend_knowledge")    // 女朋友专用索引
                .prefix("gf:")                         // gf 前缀
                .initializeSchema(true)
                .build();
    }
}