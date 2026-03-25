package com.web.demo.docs;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Document(indexName = "products")
public record ProductDocument(
        @Id   // ✅ REQUIRED
        String id,
        Long productId,
        String sku,
        String name,
        String brand,
        String category,
        @Field(type = FieldType.Nested)
        List<VariantDocument> variantDocumentList
) {
}
