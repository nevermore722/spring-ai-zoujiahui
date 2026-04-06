package org.zjh.ai.springaizoujiahui.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private final VectorStore vectorStore;

    public KnowledgeBaseInitializer(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化知识库...");

        // 检查是否已有数据，避免重复加载（可选）
        // 这里简单处理，每次都重新加载

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:docs/*");

        if (resources.length == 0) {
            log.warn("未找到文档，请在 src/main/resources/docs/ 目录下放置文件（支持 PDF、TXT 等）");
            return;
        }

        List<Document> allDocuments = new ArrayList<>();

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            log.info("加载文档: {}", filename);

            try {
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                List<Document> documents = reader.read();
                allDocuments.addAll(documents);
                log.info("  成功加载 {} 个页面/段落", documents.size());
            } catch (Exception e) {
                log.error("加载文档失败: {}", filename, e);
            }
        }

        if (allDocuments.isEmpty()) {
            log.warn("没有成功加载任何文档");
            return;
        }

        log.info("共加载 {} 个原始文档片段", allDocuments.size());

        // 使用默认配置的 TokenTextSplitter
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(allDocuments);

        log.info("文档切分为 {} 个片段", chunks.size());

        // 打印前几个片段的内容（调试用）
        chunks.stream().limit(3).forEach(chunk -> {
            String text = chunk.getText();
            String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
            log.info("片段预览: {}", preview);
        });

        // 向量化并存储到 Redis
        vectorStore.add(chunks);

        log.info("知识库初始化完成！已存储 {} 个文档片段到 Redis", chunks.size());
    }
}