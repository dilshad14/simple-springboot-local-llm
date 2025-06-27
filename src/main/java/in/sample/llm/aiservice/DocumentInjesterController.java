package in.sample.llm.aiservice;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
// import dev.langchain4j.document.loader.pdf.PdfDocumentLoader;
// If you have another PDF loader, import it here, e.g.:
// import org.apache.pdfbox.pdmodel.PDDocument;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class DocumentInjesterController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentInjesterController.class);

    @PostMapping("/ingest-pdfs")
    @ResponseBody
    public ResponseEntity<String> ingestPdfs() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:doc/*.pdf");

            List<Document> documents = new ArrayList<>();
             logger.info("Before ollamaEmbeddingModel");

    
            EmbeddingModel ollamaEmbeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text") // A lightweight embedding model
                .build();
            EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

            // Define a subSplitter to handle segments that are still too large
       
            // Main splitter with subSplitter defined
            DocumentSplitter splitter = DocumentSplitters.recursive(5000, 30, subSplitter);
            for (Resource resource : resources) {
                // Add logger to show progress
                logger.info("Processing file: {}", resource.getFilename());
                // Use a PDF loader utility to create a Document from the PDF bytes
                String text = new String(resource.getInputStream().readAllBytes());
                Metadata metadata = Metadata.from("filename", resource.getFilename());
                Document document = Document.from(text, metadata);
                documents.add(document);

            List<TextSegment> segments = splitter.splitAll(documents);
            List<Embedding> embeddings = ollamaEmbeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
            logger.info("Done Processed file: {}", resource.getFilename());
            }

            return ResponseEntity.ok("PDF documents ingested successfully: " + documents.size());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during ingestion: " + e.getMessage());
        }
    }

}
