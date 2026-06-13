package in.sample.llm.aiservice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.sample.llm.aiservice.tools.ToolsConfiguration;

@Configuration
public class AssistantConfiguration {

    private static final String SYSTEM_PROMPT =
            "You are a polite assistant. Use available tools when they can answer the user's question.";

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, ToolsConfiguration tools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    MetadataAwareContentRetriever metadataAwareContentRetriever(VectorStore vectorStore) {
        return new MetadataAwareContentRetriever(vectorStore);
    }

}
