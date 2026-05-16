package in.sample.llm.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "ollamaChatModel",
        chatMemory = "chatMemory",
        tools = "toolsConfiguration"
)
public interface Assistant {

    @SystemMessage("You are a polite assistant. Use available tools when they can answer the user's question.")
    String chat(String userMessage);
}