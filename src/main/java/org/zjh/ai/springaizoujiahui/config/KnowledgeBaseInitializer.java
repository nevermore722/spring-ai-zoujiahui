package org.zjh.ai.springaizoujiahui.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
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

        // 加载 PDF 文档
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:docs/*.pdf");

        if (resources.length == 0) {
            log.warn("未找到 PDF 文档，请在 src/main/resources/docs/ 目录下放置 PDF 文件");
            return;
        }

        List<Document> allDocuments = new ArrayList<>();

        for (Resource resource : resources) {
            log.info("加载文档: {}", resource.getFilename());

            // 直接使用 PagePdfDocumentReader，不需要配置
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            List<Document> documents = reader.read();
            allDocuments.addAll(documents);
        }

        log.info("共加载 {} 个文档页面", allDocuments.size());

        // 文档切分
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(allDocuments);

        log.info("文档切分为 {} 个片段", chunks.size());

        // 向量化并存储
        vectorStore.add(chunks);

        log.info("知识库初始化完成！已存储 {} 个文档片段到 Redis", chunks.size());
    }
}