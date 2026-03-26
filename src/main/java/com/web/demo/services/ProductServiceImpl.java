package com.web.demo.services;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.web.demo.docs.ProductDocument;
import com.web.demo.dtos.MultipleProductSearchRequest;
import com.web.demo.dtos.ProductSearchRequest;
import com.web.demo.repos.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<ProductDocument> searchMultiple(MultipleProductSearchRequest request) {

        NativeQuery query = buildQuery(
                request.keyword(),
                request.brands(),
                request.sizes(),
                request.colors(),
                request.minPrice(),
                request.maxPrice(),
                request.page(),
                request.sizeLimit(),
                request.sortBy()
        );

        return execute(query);
    }

    @Override
    public List<ProductDocument> search(ProductSearchRequest request) {

        NativeQuery query = buildQuery(
                request.keyword(),
                request.brand() != null ? List.of(request.brand()) : null,
                request.size() != null ? List.of(request.size()) : null,
                request.color() != null ? List.of(request.color()) : null,
                request.minPrice(),
                request.maxPrice(),
                request.page(),
                request.sizeLimit(),
                request.sortBy()
        );

        return execute(query);
    }

    @Override
    public List<ProductDocument> searchProducts(
            String brand,
            String size,
            String color,
            double minPrice,
            double maxPrice) {

        NativeQuery query = buildQuery(
                null,
                List.of(brand),
                List.of(size),
                List.of(color),
                minPrice,
                maxPrice,
                0,
                10,
                null
        );

        return execute(query);
    }

    @Override
    public List<ProductDocument> findByPriceRange(double minPrice, double maxPrice) {

        NativeQuery query = buildQuery(
                null,
                null,
                null,
                null,
                minPrice,
                maxPrice,
                0,
                10,
                null
        );

        return execute(query);
    }

    @Override
    public List<ProductDocument> findByVariantSize(String size) {

        NativeQuery query = buildQuery(
                null,
                null,
                List.of(size),
                null,
                null,
                null,
                0,
                10,
                null
        );

        return execute(query);
    }

    @Override
    public Iterable<ProductDocument> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public ProductDocument getProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    // ================= COMMON EXECUTOR =================
    private List<ProductDocument> execute(NativeQuery query) {
        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }

    // ================= COMMON QUERY BUILDER =================
    private NativeQuery buildQuery(
            String keyword,
            List<String> brands,
            List<String> sizes,
            List<String> colors,
            Double minPrice,
            Double maxPrice,
            Integer page,
            Integer sizeLimit,
            String sortBy
    ) {

        return NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    applyKeyword(b, keyword);
                    applyBrandFilter(b, brands);
                    applyVariantFilter(b, sizes, colors);
                    applyPrice(b, minPrice, maxPrice);

                    return b;
                }))
                .withPageable(PageRequest.of(
                        page != null ? page : 0,
                        sizeLimit != null ? sizeLimit : 10
                ))
                .withSort(s -> applySorting(s, sortBy))
                .build();
    }

    private void applyPrice(BoolQuery.Builder b, Double minPrice, Double maxPrice) {
        b.filter(f -> f.nested(n -> n
                .path("variantDocumentList")
                .query(nq -> nq.bool(nb -> {
                    // price
                    if (minPrice != null || maxPrice != null) {
                        nb.must(m -> m.range(r -> r.number(num -> {
                            num.field("variantDocumentList.price");
                            if (minPrice != null) num.gte(minPrice);
                            if (maxPrice != null) num.lte(maxPrice);
                            return num;
                        })));
                    }

                    return nb;
                }))
        ));
    }

    // ================= FILTER METHODS =================

    private void applyKeyword(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder b,
                              String keyword) {

        if (keyword != null && !keyword.isBlank()) {
            b.must(m -> m.multiMatch(mm -> mm
                    .fields("name", "brand", "category")
                    .query(keyword)
                    .fuzziness("AUTO")
            ));
        }
    }

    private void applyBrandFilter(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder b,
                                  List<String> brands) {

        if (brands != null && !brands.isEmpty()) {
            b.filter(f -> f.terms(t -> t
                    .field("brand")
                    .terms(v -> v.value(
                            brands.stream().map(FieldValue::of).toList()
                    ))
            ));
        }
    }

    private void applyVariantFilter(
            co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder b,
            List<String> sizes,
            List<String> colors) {
        if (sizes != null || colors != null) {
            b.filter(f -> f.nested(n -> n
                    .path("variantDocumentList")
                    .query(nq -> nq.bool(nb -> {

                        // size
                        if (sizes != null && !sizes.isEmpty()) {
                            nb.must(m -> m.terms(t -> t
                                    .field("variantDocumentList.size")
                                    .terms(v -> v.value(
                                            sizes.stream().map(FieldValue::of).toList()
                                    ))
                            ));
                        }
                        // color
                        if (colors != null && !colors.isEmpty()) {
                            nb.must(m -> m.terms(t -> t
                                    .field("variantDocumentList.color")
                                    .terms(v -> v.value(
                                            colors.stream().map(FieldValue::of).toList()
                                    ))
                            ));
                        }
                        return nb;
                    }))
            ));
        }
    }

    private co.elastic.clients.util.ObjectBuilder<
            co.elastic.clients.elasticsearch._types.SortOptions> applySorting(
            co.elastic.clients.elasticsearch._types.SortOptions.Builder s,
            String sortBy) {

        if ("priceAsc".equalsIgnoreCase(sortBy)) {
            return s.field(f -> f
                    .field("variantDocumentList.price")
                    .order(SortOrder.Asc)
                    .nested(n -> n.path("variantDocumentList"))
            );
        }

        if ("priceDesc".equalsIgnoreCase(sortBy)) {
            return s.field(f -> f
                    .field("variantDocumentList.price")
                    .order(SortOrder.Desc)
                    .nested(n -> n.path("variantDocumentList"))
            );
        }

        return s; // ✅ return builder itself
    }

    // ================= API METHODS =================

   /* @Override
    public List<ProductDocument> searchMultiple(MultipleProductSearchRequest request) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {

                    // 🔍 keyword
                    if (request.keyword() != null && !request.keyword().isBlank()) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .fields("name", "brand", "category")
                                .query(request.keyword())
                                .fuzziness("AUTO")
                        ));
                    }

                    // 🏷 MULTI BRAND
                    if (request.brands() != null && !request.brands().isEmpty()) {
                        b.filter(f -> f.terms(t -> t
                                .field("brand")
                                .terms(v -> v.value(
                                        request.brands().stream()
                                                .map(FieldValue::of)
                                                .toList()
                                ))
                        ));
                    }

                    // 📦 NESTED MULTI FILTERS
                    if (request.sizes() != null || request.colors() != null
                            || request.minPrice() != null || request.maxPrice() != null) {

                        b.filter(f -> f.nested(n -> n
                                .path("variantDocumentList")
                                .query(nq -> nq.bool(nb -> {

                                    // 📏 MULTI SIZE
                                    if (request.sizes() != null && !request.sizes().isEmpty()) {
                                        nb.must(m -> m.terms(t -> t
                                                .field("variantDocumentList.size")
                                                .terms(v -> v.value(
                                                        request.sizes().stream()
                                                                .map(FieldValue::of)
                                                                .toList()
                                                ))
                                        ));
                                    }

                                    // 🎨 MULTI COLOR
                                    if (request.colors() != null && !request.colors().isEmpty()) {
                                        nb.must(m -> m.terms(t -> t
                                                .field("variantDocumentList.color")
                                                .terms(v -> v.value(
                                                        request.colors().stream()
                                                                .map(FieldValue::of)
                                                                .toList()
                                                ))
                                        ));
                                    }

                                    // 💰 PRICE RANGE
                                    if (request.minPrice() != null || request.maxPrice() != null) {
                                        nb.must(m -> m.range(r -> r.number(num -> {
                                            num.field("variantDocumentList.price");
                                            if (request.minPrice() != null) {
                                                num.gte(request.minPrice());
                                            }
                                            if (request.maxPrice() != null) {
                                                num.lte(request.maxPrice());
                                            }
                                            return num;
                                        })));
                                    }

                                    return nb;
                                }))
                        ));
                    }

                    return b;
                }))

                // 📄 Pagination
                .withPageable(PageRequest.of(
                        request.page() != null ? request.page() : 0,
                        request.sizeLimit() != null ? request.sizeLimit() : 10
                ))

                // 🔽 Sorting
                .withSort(s -> {
                    if ("priceAsc".equalsIgnoreCase(request.sortBy())) {
                        return s.field(f -> f
                                .field("variantDocumentList.price")
                                .order(SortOrder.Asc)
                                .nested(n -> n
                                        .path("variantDocumentList")   // 🔥 REQUIRED
                                )
                        );
                    } else if ("priceDesc".equalsIgnoreCase(request.sortBy())) {
                        return s.field(f -> f
                                .field("variantDocumentList.price")
                                .order(SortOrder.Desc)
                                .nested(n -> n
                                        .path("variantDocumentList")   // 🔥 REQUIRED
                                )
                        );
                    }
                    return s;
                })

                .build();

        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }

    @Override
    public List<ProductDocument> search(ProductSearchRequest request) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {

                    // 🔍 Keyword search
                    if (request.keyword() != null && !request.keyword().isBlank()) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .fields("name", "brand", "category")
                                .query(request.keyword())
                                .fuzziness("AUTO")
                        ));
                    }

                    // 🏷 Brand filter
                    if (request.brand() != null && !request.brand().isBlank()) {
                        b.filter(f -> f.term(t -> t
                                .field("brand")
                                .value(request.brand())
                        ));
                    }

                    // 📦 Nested filters
                    if (request.size() != null || request.color() != null
                            || request.minPrice() != null || request.maxPrice() != null) {

                        b.filter(f -> f.nested(n -> n
                                .path("variantDocumentList")
                                .query(nq -> nq.bool(nb -> {

                                    // size
                                    if (request.size() != null && !request.size().isBlank()) {
                                        nb.must(m -> m.term(t -> t
                                                .field("variantDocumentList.size")
                                                .value(request.size())
                                        ));
                                    }

                                    // color
                                    if (request.color() != null && !request.color().isBlank()) {
                                        nb.must(m -> m.term(t -> t
                                                .field("variantDocumentList.color")
                                                .value(request.color())
                                        ));
                                    }

                                    // price range
                                    if (request.minPrice() != null || request.maxPrice() != null) {
                                        nb.must(m -> m.range(r -> r.number(num -> {
                                            num.field("variantDocumentList.price");  // ✅ FIXED
                                            if (request.minPrice() != null) {
                                                num.gte(request.minPrice());
                                            }
                                            if (request.maxPrice() != null) {
                                                num.lte(request.maxPrice());
                                            }
                                            return num;
                                        })));
                                    }

                                    return nb;
                                }))
                        ));
                    }

                    return b;
                }))

                // 📄 Pagination
                .withPageable(PageRequest.of(
                        request.page() != null ? request.page() : 0,
                        request.sizeLimit() != null ? request.sizeLimit() : 10
                ))

                // 🔽 Sorting
                .withSort(s -> {
                    if ("priceAsc".equalsIgnoreCase(request.sortBy())) {
                        return s.field(f -> f
                                .field("variantDocumentList.price")
                                .order(SortOrder.Asc)
                                .nested(n -> n
                                        .path("variantDocumentList")   // 🔥 REQUIRED
                                )
                        );
                    } else if ("priceDesc".equalsIgnoreCase(request.sortBy())) {
                        return s.field(f -> f
                                .field("variantDocumentList.price")
                                .order(SortOrder.Desc)
                                .nested(n -> n
                                        .path("variantDocumentList")   // 🔥 REQUIRED
                                )
                        );
                    }
                    return s;
                })

                .build();

        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }

    @Override
    public List<ProductDocument> searchProducts(
            String brand,
            String size,
            String color,
            double minPrice,
            double maxPrice) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b

                                // ✅ Brand filter (top-level field)
                                .must(m -> m
                                        .match(mm -> mm
                                                .field("brand")
                                                .query(brand)
                                        )
                                )

                                // ✅ Nested variant filters
                                .must(m -> m
                                        .nested(n -> n
                                                .path("variantDocumentList")
                                                .query(nq -> nq
                                                        .bool(nb -> nb

                                                                // size
                                                                .must(sm -> sm
                                                                        .match(mm -> mm
                                                                                .field("variantDocumentList.size")
                                                                                .query(size)
                                                                        )
                                                                )

                                                                // color
                                                                .must(cm -> cm
                                                                        .match(mm -> mm
                                                                                .field("variantDocumentList.color")
                                                                                .query(color)
                                                                        )
                                                                )

                                                                // price range
                                                                .must(pm -> pm
                                                                        .range(r -> r
                                                                                .number(num -> num
                                                                                        .field("variantDocumentList.price")
                                                                                        .gte(minPrice)
                                                                                        .lte(maxPrice)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .build();

        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }

    @Override
    public List<ProductDocument> findByPriceRange(double minPrice, double maxPrice) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .nested(n -> n
                                .path("variantDocumentList")
                                .query(nq -> nq
                                        .range(r -> r
                                                .number(num -> num
                                                        .field("variantDocumentList.price")
                                                        .gte(minPrice)
                                                        .lte(maxPrice)
                                                )
                                        )
                                )
                        )
                )
                .build();

        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }

    @Override
    public Iterable<ProductDocument> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public ProductDocument getProductById(String id) {
        Optional<ProductDocument> product = productRepository.findById(id);

        return product.orElse(null); // or throw exception
    }

    @Override
    public List<ProductDocument> findByVariantSize(String size) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .nested(n -> n
                                .path("variantDocumentList")
                                .query(nq -> nq
                                        .match(m -> m
                                                .field("variantDocumentList.size")
                                                .query(size)
                                        )
                                )
                        )
                )
                .build();

        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }*/
}
