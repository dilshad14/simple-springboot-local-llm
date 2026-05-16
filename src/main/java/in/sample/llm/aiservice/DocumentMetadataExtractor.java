package in.sample.llm.aiservice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.langchain4j.model.chat.ChatModel;

@Service
public class DocumentMetadataExtractor {

    private static final Logger logger = LogManager.getLogger(DocumentMetadataExtractor.class);

    @Autowired
    private ChatModel chatModel;

    public DocumentMetadata extractMetadata(String content, String filename) {
        logger.info("Extracting metadata for document: {}", filename);
        
        try {
            // Truncate content if too long to avoid token limits
            String truncatedContent = truncateContent(content, 4000);
            
            String prompt = String.format("""
                Analyze the following document content and extract metadata in JSON format.
                
                Document filename: %s
                Document content (truncated): %s
                
                Please provide a JSON response with the following fields:
                1. chapter_name: Extract or derive a meaningful chapter/topic name from the content
                2. abstract: A concise 200-word summary of the document's main content
                3. search_tags: A comma-separated list of 5-10 relevant tags/keywords derived from the content
                
                Response format (JSON only, no additional text):
                {
                    "chapter_name": "extracted chapter name",
                    "abstract": "200-word summary here",
                    "search_tags": "tag1, tag2, tag3, tag4, tag5"
                }
                """, filename, truncatedContent);

            String response = chatModel.chat(prompt);
            logger.debug("LLM response for metadata extraction: {}", response);
            
            return parseMetadataResponse(response, filename);
            
        } catch (Exception e) {
            logger.error("Error extracting metadata for document {}: {}", filename, e.getMessage(), e);
            return createFallbackMetadata(filename);
        }
    }

    private DocumentMetadata parseMetadataResponse(String response, String filename) {
        try {
            // Clean the response to extract JSON
            String jsonResponse = response.trim();
            
            // Remove any markdown formatting if present
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7);
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
            jsonResponse = jsonResponse.trim();
            
            // Simple JSON parsing (you might want to use a proper JSON library)
            String chapterName = extractJsonValue(jsonResponse, "chapter_name");
            String abstractText = extractJsonValue(jsonResponse, "abstract");
            String searchTags = extractJsonValue(jsonResponse, "search_tags");
            
            return new DocumentMetadata(
                chapterName != null ? chapterName : "Unknown Chapter",
                abstractText != null ? abstractText : "No abstract available",
                searchTags != null ? searchTags : "document, content"
            );
            
        } catch (Exception e) {
            logger.warn("Failed to parse metadata response for {}: {}", filename, e.getMessage());
            return createFallbackMetadata(filename);
        }
    }

    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.debug("Error extracting JSON value for key {}: {}", key, e.getMessage());
        }
        return null;
    }

    private DocumentMetadata createFallbackMetadata(String filename) {
        String baseName = filename.replaceAll("\\.pdf$", "").replaceAll("[-_]", " ");
        return new DocumentMetadata(
            baseName,
            "Document content extracted from " + filename,
            "document, " + baseName.toLowerCase()
        );
    }

    private String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "... [content truncated]";
    }

    public static class DocumentMetadata {
        private final String chapterName;
        private final String abstractText;
        private final String searchTags;

        public DocumentMetadata(String chapterName, String abstractText, String searchTags) {
            this.chapterName = chapterName;
            this.abstractText = abstractText;
            this.searchTags = searchTags;
        }

        public String getChapterName() {
            return chapterName;
        }

        public String getAbstract() {
            return abstractText;
        }

        public String getSearchTags() {
            return searchTags;
        }

        @Override
        public String toString() {
            return String.format("DocumentMetadata{chapterName='%s', abstract='%s', searchTags='%s'}", 
                               chapterName, abstractText, searchTags);
        }
    }
}
