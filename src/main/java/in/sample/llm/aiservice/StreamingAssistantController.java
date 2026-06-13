package in.sample.llm.aiservice;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
public class StreamingAssistantController {

    private final StreamingAssistantService streamingAssistant;

    public StreamingAssistantController(StreamingAssistantService streamingAssistant) {
        this.streamingAssistant = streamingAssistant;
    }

    @GetMapping(value = "/streamingAssistant", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamingAssistant(
            @RequestParam(value = "message", defaultValue = "What is the current time?") String message) {
        return streamingAssistant.chat(message);
    }

}
