package com.web.demo.services;

import com.web.demo.docs.ProductDocument;
import com.web.demo.dtos.MultipleProductSearchRequest;
import com.web.demo.dtos.ProductSearchRequest;

import java.util.List;

public interface ProductService {

    List<ProductDocument> searchMultiple(MultipleProductSearchRequest request);

    List<ProductDocument> search(ProductSearchRequest request);

    List<ProductDocument> searchProducts(
            String brand,
            String size,
            String color,
            double minPrice,
            double maxPrice);

    List<ProductDocument> findByPriceRange(double minPrice, double maxPrice);

    Iterable<ProductDocument> getAllProducts();

    ProductDocument getProductById(String id);

    List<ProductDocument> findByVariantSize(String size);
}
