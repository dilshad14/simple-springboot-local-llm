package in.sample.llm.aiservice;


import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import in.sample.llm.lowlevel.ChatModelController;

@Configuration
public class AssistantConfiguration {

    /**
     * This chat memory will be used by {@link Assistant} and {@link StreamingAssistant}
     */
    @Bean
    @Scope(SCOPE_PROTOTYPE)
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages( 20);
    }

    /**
     * This listener will be injected into every {@link ChatModel} and {@link StreamingChatModel}
     * bean found in the application context.
     * It will listen for {@link ChatModel} in the {@link ChatModelController} as well as
     * {@link Assistant} and {@link StreamingAssistant}.
     */
    @Bean
    ChatModelListener chatModelListener() {
        return new MyChatModelListener();
    }

    /**
     * Embedding model for RAG functionality
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text")  // Good embedding model for Ollama
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore () {
        return  new InMemoryEmbeddingStore<>();
    }

    @Bean
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")  // Ollama default endpoint
                //.modelName("gemma3:1b")
                .modelName("llama3.2:latest")
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }

    @Bean
    public OllamaStreamingChatModel ollamaStreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")  // Ollama default endpoint
               // .modelName("gemma3:1b")
                .modelName("llama3.2:latest")
                .httpClientBuilder(JdkHttpClient.builder())
                .build();
    }

    /**
     * RAG retriever used only by {@link RagChatController} — not auto-wired into {@link Assistant}.
     */
    @Bean
    public ContentRetriever metadataAwareContentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        return new MetadataAwareContentRetriever(embeddingStore, embeddingModel);
    }

}
