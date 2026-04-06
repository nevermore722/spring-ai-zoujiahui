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
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private final VectorStore boyfriendVectorStore;
    private final VectorStore girlfriendVectorStore;

    public KnowledgeBaseInitializer(
            @Qualifier("boyfriendVectorStore") VectorStore boyfriendVectorStore,
            @Qualifier("girlfriendVectorStore") VectorStore girlfriendVectorStore) {
        this.boyfriendVectorStore = boyfriendVectorStore;
        this.girlfriendVectorStore = girlfriendVectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 初始化男朋友知识库
        initKnowledgeBase(boyfriendVectorStore, "classpath:docs/boyfriend/*.txt", "男朋友");
        initKnowledgeBase(boyfriendVectorStore, "classpath:docs/boyfriend/*.pdf", "男朋友");

        // 初始化女朋友知识库
        initKnowledgeBase(girlfriendVectorStore, "classpath:docs/girlfriend/*.txt", "女朋友");
        initKnowledgeBase(girlfriendVectorStore, "classpath:docs/girlfriend/*.pdf", "女朋友");
    }

    private void initKnowledgeBase(VectorStore vectorStore, String pattern, String name) {
        log.info("开始初始化{}知识库...", name);

        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(pattern);

            if (resources.length == 0) {
                log.info("未找到{}知识文档（pattern: {}）", name, pattern);
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
                log.warn("没有成功加载任何{}知识文档", name);
                return;
            }

            log.info("共加载 {} 个原始文档片段", allDocuments.size());

            // 文档切分
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.apply(allDocuments);

            log.info("文档切分为 {} 个片段", chunks.size());

            // 打印前几个片段预览
            chunks.stream().limit(3).forEach(chunk -> {
                String text = chunk.getText();
                String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                log.info("片段预览: {}", preview);
            });

            // 向量化并存储
            vectorStore.add(chunks);

            log.info("{}知识库初始化完成！已存储 {} 个文档片段", name, chunks.size());

        } catch (IOException e) {
            log.error("读取{}知识库文件失败", name, e);
        }
    }
}