package org.zjh.ai.springaizoujiahui.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BoyfriendKnowledgeInitializer implements ApplicationRunner {

    private final VectorStore vectorStore;

    public BoyfriendKnowledgeInitializer(
            @Qualifier("boyfriendVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化男朋友知识库...");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 男朋友知识放在 docs/boyfriend/ 目录下
        Resource[] resources = resolver.getResources("classpath:docs/boyfriend/*");

        if (resources.length == 0) {
            log.warn("未找到男朋友知识文档，请在 src/main/resources/docs/boyfriend/ 目录下放置文件");
            return;
        }

        List<Document> allDocuments = new ArrayList<>();

        for (Resource resource : resources) {
            log.info("加载文档: {}", resource.getFilename());
            try {
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                allDocuments.addAll(reader.read());
            } catch (Exception e) {
                log.error("加载失败: {}", resource.getFilename(), e);
            }
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(allDocuments);

        vectorStore.add(chunks);
        log.info("男朋友知识库初始化完成！已存储 {} 个文档片段", chunks.size());
    }
}