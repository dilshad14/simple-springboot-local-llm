package in.sample.llm.aiservice;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

@RestController
@RequestMapping("/api")
public class DocumentInjecterController {

    private static final Logger logger = LogManager.getLogger(DocumentInjecterController.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @PostMapping("/inject-rag")
    public String injectDocuments() {
        logger.info("Starting document injection process");
        try {
            // Get all PDF files from the classpath
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] pdfResources = resolver.getResources("classpath:rag-docs/*.pdf");

            if (pdfResources.length == 0) {
                logger.warn("No PDF files found in resources/rag-docs directory");
                return "No PDF files found in resources/rag-docs directory";
            }

            logger.info("Found {} PDF files to process", pdfResources.length);
            int totalSegments = 0;
            DocumentSplitter documentSplitter = DocumentSplitters.recursive(300, 50);

            for (Resource resource : pdfResources) {
                try {
                    logger.debug("Processing file: {}", resource.getFilename());
                    
                    // Read PDF content
                    String content = readPdfContent(resource);
                    
                    if (content == null || content.trim().isEmpty()) {
                        logger.warn("Skipping empty file: {}", resource.getFilename());
                        continue; // Skip empty files
                    }
                    
                    logger.debug("File {} content length: {} characters", resource.getFilename(), content.length());
                    
                    // Create document with metadata
                    Metadata metadata = Metadata.from("filename", resource.getFilename());
                    metadata.put("date", LocalDateTime.now().toString());
                    Document document = Document.from(content, metadata);
                    
                    // Split document into segments
                    List<TextSegment> segments = documentSplitter.split(document);
                    logger.debug("File {} split into {} segments", resource.getFilename(), segments.size());
                    
                    // Create embeddings and store them
                    for (TextSegment segment : segments) {
                        Embedding embedding = embeddingModel.embed(segment).content();
                        embeddingStore.add(embedding, segment);
                        totalSegments++;
                    }
                    
                    logger.info("Successfully processed file: {} with {} segments", resource.getFilename(), segments.size());
                    
                } catch (Exception e) {
                    // Log error but continue with other files
                    logger.error("Error processing file {}: {}", resource.getFilename(), e.getMessage(), e);
                }
            }

            logger.info("Document injection completed. Processed {} files and created {} segments", 
                       pdfResources.length, totalSegments);
            
            return String.format("Successfully processed %d PDF files and created %d segments", 
                               pdfResources.length, totalSegments);

        } catch (Exception e) {
            logger.error("Error during document injection process: {}", e.getMessage(), e);
            return "Error processing documents: " + e.getMessage();
        }
    }

    private String readPdfContent(Resource resource) throws IOException {
        // Use PDFBox to extract text from PDF
        try (PDDocument document = PDDocument.load(resource.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String content = pdfStripper.getText(document);
            logger.debug("Successfully extracted text from file: {} ({} characters)", resource.getFilename(), content.length());
            return content;
        } catch (IOException e) {
            logger.error("Failed to extract text from file: {}", resource.getFilename(), e);
            throw new IOException("Failed to extract text from file: " + resource.getFilename(), e);
        }
    }

    // Getter for the embedding store to be used by other services
    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        logger.debug("Retrieving embedding store");
        return embeddingStore;
    }
}
