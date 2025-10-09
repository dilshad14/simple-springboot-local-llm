package in.sample.llm.aiservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class RagChatController {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ContentRetriever contentRetriever;

    @GetMapping("/rag-chat")
    public String chatWithRag(@RequestParam(value = "message", defaultValue = "Hello!") String message) {
        try {
            // Create a Query object
            Query query = Query.from(message);
            
            // Use the ContentRetriever to get relevant content
            List<Content> relevantContents = contentRetriever.retrieve(query);
            
            // Build context from relevant contents
            StringBuilder context = new StringBuilder();
            context.append("Based on the following documents, please answer the question:\n\n");
            
            for (Content content : relevantContents) {
                context.append("Document excerpt: ").append(content.textSegment().text()).append("\n\n");
            }
            
            context.append("Question: ").append(message).append("\n\n");
            context.append("Please provide an accurate answer based on the document excerpts above. " +
                          "If the documents don't contain enough information to answer the question, " +
                          "please say so.");
            
            // Get response from chat model
            return chatModel.chat(context.toString());
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
} 