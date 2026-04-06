package org.zjh.ai.springaizoujiahui.controller;

import org.springframework.web.bind.annotation.*;
import org.zjh.ai.springaizoujiahui.service.RagService;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam String question) {
        String answer = ragService.ask(question);
        return Map.of(
                "question", question,
                "answer", answer
        );
    }
}