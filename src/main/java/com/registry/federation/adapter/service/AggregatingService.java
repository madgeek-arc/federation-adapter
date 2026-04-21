/*
 * Copyright 2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.registry.federation.adapter.service;

import com.registry.federation.adapter.Page;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AggregatingService {

    private static final Logger logger = LoggerFactory.getLogger(AggregatingService.class);

    private final RestClient restClient;
    private final NodeEndpointService nodeEndpointService;
    private final NodeResolver nodeResolver;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    public AggregatingService(RestClient restClient,
                              NodeEndpointService nodeEndpointService,
                              NodeResolver nodeResolver) {
        this.restClient = restClient;
        this.nodeEndpointService = nodeEndpointService;
        this.nodeResolver = nodeResolver;
    }

    public Page<Object> getMergedPagedResults(FacetFilter ff) {
        int from = ff.getFrom();
        int quantity = ff.getQuantity();
        int to = from + quantity;

        List<String> endpoints = nodeEndpointService.getResourceCatalogueEndpoints();
        List<APIPageMetadata> apiMetadataList = new ArrayList<>();
        int totalAvailable = 0;
        List<Facet> allFacets = new ArrayList<>();

        for (String endpoint : endpoints) {
            try {
                String metaUrl = buildUrlWithFacetFilter(endpoint, ff, 0, maxQuantity);

                Paging<?> page = restClient.get()
                        .uri(metaUrl)
                        .retrieve()
                        .body(Paging.class);

                if (page == null) continue;

                int size = page.getTotal();
                totalAvailable += size;

                apiMetadataList.add(new APIPageMetadata(endpoint, size));
                allFacets.addAll(Optional.ofNullable(page.getFacets()).orElse(Collections.emptyList()));

            } catch (Exception e) {
                logger.info("Metadata fetch failed for: {}", endpoint, e);
            }
        }

        // fetch results
        List<Object> finalResults = new ArrayList<>();
        int globalIndex = 0;

        for (APIPageMetadata meta : apiMetadataList) {

            if (globalIndex + meta.size <= from) {
                globalIndex += meta.size;
                continue;
            }

            int sliceFrom = Math.max(0, from - globalIndex);
            int sliceTo = Math.min(meta.size, to - globalIndex);
            int sliceCount = sliceTo - sliceFrom;

            if (sliceCount > 0) {
                try {
                    String dataUrl = buildUrlWithFacetFilter(meta.url, ff, sliceFrom, sliceCount);

                    Paging<?> page = restClient.get()
                            .uri(dataUrl)
                            .retrieve()
                            .body(Paging.class);

                    if (page != null && page.getResults() != null) {
                        finalResults.addAll(page.getResults());
                    }

                } catch (Exception e) {
                    logger.info("Failed to fetch data from: {}", meta.url, e);
                }
            }

            globalIndex += meta.size;

            if (finalResults.size() >= quantity) break;
        }

        // merge facets
        List<Facet> mergedFacets = mergeFacets(allFacets);

        // creating paging
        return createPage(from, finalResults.size(), totalAvailable, finalResults, mergedFacets);
    }

    private String buildUrlWithFacetFilter(String baseUrl, FacetFilter ff, int from, int quantity) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("from", from)
                .queryParam("quantity", quantity);

        if (ff.getKeyword() != null && !ff.getKeyword().isEmpty()) {
            builder.queryParam("keyword", ff.getKeyword());
        }

        if (ff.getOrderBy() != null && !ff.getOrderBy().isEmpty()) {
            for (Map.Entry<String, Object> entry : ff.getOrderBy().entrySet()) {
                String sortField = entry.getKey();
                String order = "asc";

                Object value = entry.getValue();

                if (value instanceof Map<?, ?> innerMap) {
                    Object orderVal = innerMap.get("order");
                    if (orderVal != null) {
                        order = orderVal.toString().toLowerCase();
                    }
                } else if (value instanceof String) {
                    order = ((String) value).toLowerCase();
                }

                builder.queryParam("sort", sortField);
                builder.queryParam("order", order);
                break; // only first entry used
            }
        }

        if (ff.getFilter() != null && !ff.getFilter().isEmpty()) {
            for (Map.Entry<String, Object> entry : ff.getFilter().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    for (Object item : (List<?>) value) {
                        builder.queryParam(entry.getKey(), item.toString());
                    }
                } else {
                    builder.queryParam(entry.getKey(), value.toString());
                }
            }
        }

        return builder.toUriString();
    }

    private List<Facet> mergeFacets(List<Facet> allFacets) {
        Map<String, Facet> mergedFacetMap = new LinkedHashMap<>();

        for (Facet facet : allFacets) {
            String field = facet.getField();
            String label = facet.getLabel();

            Facet existingFacet = mergedFacetMap.computeIfAbsent(field, f -> {
                Facet newFacet = new Facet();
                newFacet.setField(field);
                newFacet.setLabel(label);
                newFacet.setValues(new ArrayList<>());
                return newFacet;
            });

            // merge facet values
            Map<String, Value> existingValuesMap = existingFacet.getValues().stream()
                    .collect(Collectors.toMap(Value::getValue, fv -> fv, (a, b) -> a));

            for (Value incoming : facet.getValues()) {
                existingValuesMap.merge(
                        incoming.getValue(),
                        incoming,
                        (existing, inc) -> {
                            existing.setCount(existing.getCount() + inc.getCount());
                            return existing;
                        }
                );
            }

            existingFacet.setValues(new ArrayList<>(existingValuesMap.values()));
        }

        return new ArrayList<>(mergedFacetMap.values());
    }

    private Page<Object> createPage(int from, int resultsSize, int total, List<Object> results, List<Facet> facets) {
        Page<Object> page = new Page<>(total, from, from+resultsSize, results, facets);
        page.setMetadata(Map.of("nodes", nodeResolver.fetchNodes()));
        return page;
    }

    private static class APIPageMetadata {
        String url;
        int size;

        APIPageMetadata(String url, int size) {
            this.url = url;
            this.size = size;
        }
    }
}
