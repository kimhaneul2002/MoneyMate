package com.example.moneymate.service;

import com.example.moneymate.entity.LedgerItem;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAnalysisService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    public String analyze(List<LedgerItem> items) {

        if (items.isEmpty()) {
            return "지출 내역이 없어 분석할 수 없습니다.";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 이번 달 가계부 내역입니다:\n");
        for (LedgerItem item : items) {
            prompt.append("- ")
                    .append(item.getCategory().getName())
                    .append(": ")
                    .append(item.getAmount())
                    .append("원\n");
        }
        prompt.append("\n위 데이터를 바탕으로 다음을 분석해주세요:\n");
        prompt.append("1. 소비 습관 분석\n");
        prompt.append("2. 절약 팁 추천\n");
        prompt.append("3. 다음 달 예산 추천\n");
        prompt.append("4. 소비 습관 점수 (100점 만점)\n");

        OpenAiService service = new OpenAiService(openAiApiKey);

        CompletionRequest request = CompletionRequest.builder()
                .model("gpt-3.5-turbo-instruct")
                .prompt(prompt.toString())
                .maxTokens(1000)
                .build();

        return service.createCompletion(request)
                .getChoices()
                .get(0)
                .getText();
    }
}