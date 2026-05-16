package in.sample.llm.aiservice;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;

//@Component
public class MetadataAwareContentRetriever implements ContentRetriever {

    private static final Logger logger = LogManager.getLogger(MetadataAwareContentRetriever.class);

    private final ContentRetriever baseRetriever;

    public MetadataAwareContentRetriever(
            @Autowired EmbeddingStore<TextSegment> embeddingStore,
            @Autowired EmbeddingModel embeddingModel) {
        this.baseRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(10)
                .minScore(0.6)
                .build();
    }

    @Override
    public List<Content> retrieve(Query query) {
        logger.info("Retrieving content for query: {}", query.text());
        
        // Parse metadata search criteria from query
        MetadataSearchCriteria criteria = parseMetadataQuery(query.text());
        
        // Get results from base retriever
        List<Content> allResults = baseRetriever.retrieve(query);
        
        // If no metadata criteria, return all results
        // if (!criteria.hasMetadataSearch()) {
        //     logger.info("No metadata criteria found, returning {} results", allResults.size());
        //     return allResults;
        // }
        
        // Filter results by metadata
        logger.info("Filtering results by metadata criteria: {}", criteria);
        List<Content> filteredResults = new ArrayList<>();
        for (Content content : allResults) {
            if (matchesMetadataCriteria(content.textSegment(), criteria)) {
                filteredResults.add(content);
            }
        }
        
        logger.info("Found {} results matching metadata criteria", filteredResults.size());
        return filteredResults.isEmpty() ? allResults : filteredResults;
    }

    private MetadataSearchCriteria parseMetadataQuery(String queryText) {
        MetadataSearchCriteria criteria = new MetadataSearchCriteria();
        
        // Pattern for tag search: "tags: physics, electricity" or "tag: physics"
        Pattern tagPattern = Pattern.compile("(?:tags?|tag)\\s*:\\s*([^,]+(?:,\\s*[^,]+)*)", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagPattern.matcher(queryText);
        if (tagMatcher.find()) {
            String[] tags = tagMatcher.group(1).split(",");
            for (String tag : tags) {
                criteria.addTag(tag.trim());
            }
        }
        
        // Pattern for chapter search: "chapter: physics" or "chapter_name: physics"
        Pattern chapterPattern = Pattern.compile("(?:chapter|chapter_name)\\s*:\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
        Matcher chapterMatcher = chapterPattern.matcher(queryText);
        if (chapterMatcher.find()) {
            criteria.setChapterName(chapterMatcher.group(1).trim());
        }
        
        // Pattern for filename search: "file: physics.pdf" or "filename: physics"
        Pattern filePattern = Pattern.compile("(?:file|filename)\\s*:\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
        Matcher fileMatcher = filePattern.matcher(queryText);
        if (fileMatcher.find()) {
            criteria.setFilename(fileMatcher.group(1).trim());
        }
        
        return criteria;
    }

    private boolean matchesMetadataCriteria(TextSegment segment, MetadataSearchCriteria criteria) {
        if (segment.metadata() == null) {
            return false;
        }
        
        // Check chapter name match
        if (criteria.getChapterName() != null) {
            String chapterName = segment.metadata().getString("chapter_name");
            if (chapterName == null || !chapterName.toLowerCase().contains(criteria.getChapterName().toLowerCase())) {
                return false;
            }
        }
        
        // Check filename match
        if (criteria.getFilename() != null) {
            String filename = segment.metadata().getString("filename");
            if (filename == null || !filename.toLowerCase().contains(criteria.getFilename().toLowerCase())) {
                return false;
            }
        }
        
        // Check tags match
        if (!criteria.getTags().isEmpty()) {
            String searchTags = segment.metadata().getString("search_tags");
            if (searchTags == null) {
                return false;
            }
            
            boolean hasMatchingTag = false;
            for (String tag : criteria.getTags()) {
                if (searchTags.toLowerCase().contains(tag.toLowerCase())) {
                    hasMatchingTag = true;
                    break;
                }
            }
            if (!hasMatchingTag) {
                return false;
            }
        }
        
        return true;
    }

    public static class MetadataSearchCriteria {
        private String chapterName;
        private String filename;
        private final List<String> tags = new ArrayList<>();

        public void addTag(String tag) {
            this.tags.add(tag);
        }

        public void setChapterName(String chapterName) {
            this.chapterName = chapterName;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getChapterName() {
            return chapterName;
        }

        public String getFilename() {
            return filename;
        }

        public List<String> getTags() {
            return tags;
        }

        public boolean hasMetadataSearch() {
            return chapterName != null || filename != null || !tags.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("MetadataSearchCriteria{chapterName='%s', filename='%s', tags=%s}", 
                               chapterName, filename, tags);
        }
    }
}
