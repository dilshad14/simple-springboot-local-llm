package in.sample.llm.aiservice;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class RagChatController {

    private final AssistantService assistant;
    private final MetadataAwareContentRetriever contentRetriever;

    public RagChatController(AssistantService assistant, MetadataAwareContentRetriever contentRetriever) {
        this.assistant = assistant;
        this.contentRetriever = contentRetriever;
    }

    @GetMapping("/rag-chat")
    public String chatWithRag(@RequestParam(value = "message", defaultValue = "Hello!") String message) {
        try {
            List<Document> relevantContents = contentRetriever.retrieve(message);

            StringBuilder context = new StringBuilder();
            context.append("Based on the following documents, please answer the question:\n\n");

            for (Document document : relevantContents) {
                context.append("Document excerpt: ").append(document.getText()).append("\n");

                Map<String, Object> metadata = document.getMetadata();
                if (metadata != null) {
                    String chapterName = metadataValue(metadata, "chapter_name");
                    String searchTags = metadataValue(metadata, "search_tags");
                    if (chapterName != null && !chapterName.isEmpty()) {
                        context.append("Chapter: ").append(chapterName).append("\n");
                    }
                    if (searchTags != null && !searchTags.isEmpty()) {
                        context.append("Tags: ").append(searchTags).append("\n");
                    }
                }
                context.append("\n");
            }

            context.append("Question: ").append(message).append("\n\n");
            context.append("Please provide an accurate answer based on the document excerpts above. " +
                    "If the documents don't contain enough information to answer the question, " +
                    "please say so. Provide response in markdown formatting.\n\n");

            if (relevantContents.isEmpty()) {
                context.append("**Search Tips:** You can also search by:\n");
                context.append("- Tags: `tags: physics, electricity`\n");
                context.append("- Chapter: `chapter: physics`\n");
                context.append("- Filename: `file: physics.pdf`\n");
                context.append("- Combined: `What is electricity? tags: physics`\n");
            }

            return assistant.chat(context.toString());

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/rag-metadata")
    public String getAvailableMetadata() {
        try {
            StringBuilder response = new StringBuilder();
            response.append("# Available Document Metadata\n\n");

            response.append("## Search Examples:\n");
            response.append("- `tags: physics, electricity` - Search by tags\n");
            response.append("- `chapter: physics` - Search by chapter name\n");
            response.append("- `file: physics.pdf` - Search by filename\n");
            response.append("- `What is electricity? tags: physics` - Combine content and metadata search\n\n");

            response.append("## How to Use Metadata Search:\n");
            response.append("1. **Tag Search**: Use `tags: keyword1, keyword2` to find documents with specific tags\n");
            response.append("2. **Chapter Search**: Use `chapter: chapter_name` to find content from specific chapters\n");
            response.append("3. **File Search**: Use `file: filename` to find content from specific files\n");
            response.append("4. **Combined Search**: Mix content queries with metadata filters\n\n");

            response.append("## Example Queries:\n");
            response.append("- `What is quantum mechanics? tags: physics`\n");
            response.append("- `Explain electricity chapter: physics`\n");
            response.append("- `Find information about waves file: physics.pdf`\n");
            response.append("- `tags: mathematics, algebra`\n");

            return response.toString();

        } catch (Exception e) {
            return "Error retrieving metadata: " + e.getMessage();
        }
    }

    private String metadataValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }

}
