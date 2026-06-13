package in.sample.llm.aiservice;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DocumentInjecterController {

    private static final Logger logger = LogManager.getLogger(DocumentInjecterController.class);

    private final VectorStore vectorStore;
    private final DocumentMetadataExtractor metadataExtractor;

    public DocumentInjecterController(VectorStore vectorStore, DocumentMetadataExtractor metadataExtractor) {
        this.vectorStore = vectorStore;
        this.metadataExtractor = metadataExtractor;
    }

    @PostMapping("/inject-rag")
    public String injectDocuments() {
        logger.info("Starting document injection process");
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] pdfResources = resolver.getResources("classpath:rag-docs/*.pdf");

            if (pdfResources.length == 0) {
                logger.warn("No PDF files found in resources/rag-docs directory");
                return "No PDF files found in resources/rag-docs directory";
            }

            logger.info("Found {} PDF files to process", pdfResources.length);
            int totalSegments = 0;
            TokenTextSplitter documentSplitter = TokenTextSplitter.builder()
                    .withChunkSize(100)
                    .withMinChunkSizeChars(50)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10_000)
                    .withKeepSeparator(true)
                    .build();

            for (Resource resource : pdfResources) {
                try {
                    logger.debug("Processing file: {}", resource.getFilename());

                    String content = readPdfContent(resource);

                    if (content == null || content.trim().isEmpty()) {
                        logger.warn("Skipping empty file: {}", resource.getFilename());
                        continue;
                    }

                    logger.debug("File {} content length: {} characters", resource.getFilename(), content.length());

                    DocumentMetadataExtractor.DocumentMetadata extractedMetadata =
                            metadataExtractor.extractMetadata(content, resource.getFilename());

                    logger.info("Extracted metadata for {}: {}", resource.getFilename(), extractedMetadata);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("filename", resource.getFilename());
                    metadata.put("date", LocalDateTime.now().toString());
                    metadata.put("chapter_name", extractedMetadata.getChapterName());
                    metadata.put("abstract", extractedMetadata.getAbstract());
                    metadata.put("search_tags", extractedMetadata.getSearchTags());

                    Document document = Document.builder()
                            .text(content)
                            .metadata(metadata)
                            .build();

                    List<Document> segments = documentSplitter.apply(List.of(document));
                    logger.debug("File {} split into {} segments", resource.getFilename(), segments.size());

                    vectorStore.add(segments);
                    totalSegments += segments.size();

                    logger.info("Successfully processed file: {} with {} segments", resource.getFilename(), segments.size());

                } catch (Exception e) {
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

}
