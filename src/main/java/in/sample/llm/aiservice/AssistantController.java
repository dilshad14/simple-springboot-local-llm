package in.sample.llm.aiservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.langchain4j.service.spring.AiService;

/**
 * This is an example of using an {@link AiService}, a high-level LangChain4j API.
 */
@RestController
public class AssistantController {

    private final Assistant assistant;

    public AssistantController(Assistant assistant) {
        this.assistant = assistant;
       
    }

    @GetMapping("/assistant")
    public String assistant(@RequestParam(value = "message", defaultValue = "What is the current time?") String message) {
        return assistant.chat(message);
    }

}
