package in.sample.llm.aiservice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class StreamingAssistantService {

    private static final String CONVERSATION_ID = "default";

    private final ChatClient chatClient;

    public StreamingAssistantService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Flux<String> chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
                .stream()
                .content();
    }

}
