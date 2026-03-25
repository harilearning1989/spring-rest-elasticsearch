package com.web.demo.controls;

import com.web.demo.docs.ProductDocument;
import com.web.demo.dtos.MultipleProductSearchRequest;
import com.web.demo.dtos.ProductSearchRequest;
import com.web.demo.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;

    @PostMapping("/searchMultiple")
    public List<ProductDocument> searchMultiple(@RequestBody MultipleProductSearchRequest request) {
        return productService.searchMultiple(request);
    }

    @PostMapping("/search")
    public List<ProductDocument> search(@RequestBody ProductSearchRequest request) {
        return productService.search(request);
    }

    @GetMapping("/search/limited")
    public List<ProductDocument> search(
            @RequestParam String brand,
            @RequestParam String size,
            @RequestParam String color,
            @RequestParam double minPrice,
            @RequestParam double maxPrice) {

        return productService.searchProducts(brand, size, color, minPrice, maxPrice);
    }

    @GetMapping("/search/price")
    public List<ProductDocument> getByPriceRange(
            @RequestParam double min,
            @RequestParam double max) {
        return productService.findByPriceRange(min, max);
    }

    @GetMapping
    public Iterable<ProductDocument> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductDocument getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }

    @GetMapping("/search/size")
    public List<ProductDocument> getBySize(@RequestParam String size) {
        return productService.findByVariantSize(size);
    }
}
