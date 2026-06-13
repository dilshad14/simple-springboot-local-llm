package in.sample.llm.aiservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

public class MetadataAwareContentRetriever {

    private static final Logger logger = LogManager.getLogger(MetadataAwareContentRetriever.class);

    private final VectorStore vectorStore;

    public MetadataAwareContentRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String query) {
        logger.info("Retrieving content for query: {}", query);

        MetadataSearchCriteria criteria = parseMetadataQuery(query);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(10)
                .similarityThreshold(0.6)
                .build();

        List<Document> allResults = vectorStore.similaritySearch(searchRequest);

        logger.info("Filtering results by metadata criteria: {}", criteria);
        List<Document> filteredResults = new ArrayList<>();
        for (Document document : allResults) {
            if (matchesMetadataCriteria(document, criteria)) {
                filteredResults.add(document);
            }
        }

        logger.info("Found {} results matching metadata criteria", filteredResults.size());
        return filteredResults.isEmpty() ? allResults : filteredResults;
    }

    private MetadataSearchCriteria parseMetadataQuery(String queryText) {
        MetadataSearchCriteria criteria = new MetadataSearchCriteria();

        Pattern tagPattern = Pattern.compile("(?:tags?|tag)\\s*:\\s*([^,]+(?:,\\s*[^,]+)*)", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagPattern.matcher(queryText);
        if (tagMatcher.find()) {
            String[] tags = tagMatcher.group(1).split(",");
            for (String tag : tags) {
                criteria.addTag(tag.trim());
            }
        }

        Pattern chapterPattern = Pattern.compile("(?:chapter|chapter_name)\\s*:\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
        Matcher chapterMatcher = chapterPattern.matcher(queryText);
        if (chapterMatcher.find()) {
            criteria.setChapterName(chapterMatcher.group(1).trim());
        }

        Pattern filePattern = Pattern.compile("(?:file|filename)\\s*:\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
        Matcher fileMatcher = filePattern.matcher(queryText);
        if (fileMatcher.find()) {
            criteria.setFilename(fileMatcher.group(1).trim());
        }

        return criteria;
    }

    private boolean matchesMetadataCriteria(Document document, MetadataSearchCriteria criteria) {
        Map<String, Object> metadata = document.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }

        if (criteria.getChapterName() != null) {
            String chapterName = stringValue(metadata.get("chapter_name"));
            if (chapterName == null || !chapterName.toLowerCase().contains(criteria.getChapterName().toLowerCase())) {
                return false;
            }
        }

        if (criteria.getFilename() != null) {
            String filename = stringValue(metadata.get("filename"));
            if (filename == null || !filename.toLowerCase().contains(criteria.getFilename().toLowerCase())) {
                return false;
            }
        }

        if (!criteria.getTags().isEmpty()) {
            String searchTags = stringValue(metadata.get("search_tags"));
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

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
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

        @Override
        public String toString() {
            return String.format("MetadataSearchCriteria{chapterName='%s', filename='%s', tags=%s}",
                    chapterName, filename, tags);
        }
    }

}
