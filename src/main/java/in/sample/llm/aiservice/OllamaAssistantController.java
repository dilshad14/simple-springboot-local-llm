package in.sample.llm.aiservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OllamaAssistantController {

    private final AssistantService assistant;

    public OllamaAssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @GetMapping("/ollama-assistant")
    public String assistant(@RequestParam(value = "message", defaultValue = "What is the current time?") String message) {
        return assistant.chat(message);
    }

}
