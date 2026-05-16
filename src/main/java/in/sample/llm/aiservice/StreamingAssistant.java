package in.sample.llm.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        streamingChatModel = "ollamaStreamingChatModel",
        chatMemory = "chatMemory",
        tools = "toolsConfiguration"
)
public interface StreamingAssistant {

    @SystemMessage("You are a polite assistant. Use available tools when they can answer the user's question.")
    Flux<String> chat(String userMessage);
}