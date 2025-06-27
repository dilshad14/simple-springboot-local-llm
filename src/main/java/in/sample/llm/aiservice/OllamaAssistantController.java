package in.sample.llm.aiservice;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.spring.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * This is an example of using an {@link AiService}, a high-level LangChain4j API.
 */
@RestController
public class OllamaAssistantController {

    private final StreamingAssistant streamingAssistant;

    private final OllamaStreamingChatModel ollamaStreamingChatModel;

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;



    public OllamaAssistantController(ChatModel chatModel, StreamingChatModel streamingChatModel, OllamaStreamingChatModel ollamaStreamingChatModel, StreamingAssistant streamingAssistant) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.ollamaStreamingChatModel = ollamaStreamingChatModel;
        this.streamingAssistant = streamingAssistant;
    }

    @GetMapping("/ollama-assistant")
    public String assistant(@RequestParam(value = "message", defaultValue = "What is the current time?") String message) {
        return chatModel.chat(message);
    }


    @GetMapping(value = "/ollama-stream-assistant", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamingAssistant(
            @RequestParam(value = "message", defaultValue = "What is the current time?") String message) {
        return streamingAssistant.chat(message);
    }
}
