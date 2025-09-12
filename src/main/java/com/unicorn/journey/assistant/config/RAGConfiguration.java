package com.unicorn.journey.assistant.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

@Configuration
public class RAGConfiguration {


    //@Resource
    //private EmbeddingModel qwenEmbeddingModel;
    //
    //@Bean
    //public EmbeddingStore<TextSegment> embeddingStore() {
    //    return new InMemoryEmbeddingStore<>();
    //}

    //@Bean
    //public ContentRetriever contentRetriever() {
    //    // load documents
    //    List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/documents/");
    //    // Text chunking
    //    DocumentByParagraphSplitter documentByParagraphSplitter =
    //            new DocumentByParagraphSplitter(512, 50);
    //    // Load data into vector database
    //    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
    //            .documentSplitter(documentByParagraphSplitter)
    //            .textSegmentTransformer(textSegment ->
    //                    TextSegment.from(textSegment.metadata().getString("file_name") +
    //                            "\n" + textSegment.text(), textSegment.metadata()))
    //            .embeddingModel(qwenEmbeddingModel)
    //            .embeddingStore(embeddingStore())
    //            .build();
    //    ingestor.ingest(documents);
    //    // Content loader
    //    return EmbeddingStoreContentRetriever.builder()
    //            .embeddingStore(embeddingStore())
    //            .embeddingModel(qwenEmbeddingModel)
    //            .maxResults(3)
    //            .minScore(0.8)
    //            .build();
    //}
}
