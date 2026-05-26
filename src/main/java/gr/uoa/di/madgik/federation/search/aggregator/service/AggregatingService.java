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

package gr.uoa.di.madgik.federation.search.aggregator.service;

import gr.uoa.di.madgik.federation.search.aggregator.Page;
import gr.uoa.di.madgik.federation.search.aggregator.model.Node;
import gr.uoa.di.madgik.federation.search.aggregator.model.NodeFacetValue;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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

    private static final String FALLBACK_PID = "21.T15999/EOSC-BEYOND";

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
            fetchPageMetadata(endpoint, ff).ifPresent(metadata -> {
                apiMetadataList.add(metadata);
                allFacets.addAll(metadata.facets);
            });
        }

        totalAvailable = apiMetadataList.stream()
                .mapToInt(metadata -> metadata.size)
                .sum();

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
                String dataUrl = buildUrlWithFacetFilter(meta.url, ff, sliceFrom, sliceCount);
                Optional<Paging<?>> pageOptional = fetchResultsPage(dataUrl);

                if (pageOptional.isPresent()) {
                    Paging<?> page = pageOptional.get();
                    if (page.getResults() != null) {
                        finalResults.addAll(page.getResults());
                    }
                }
            }

            globalIndex += meta.size;

            if (finalResults.size() >= quantity) break;
        }

        // merge facets
        List<Facet> mergedFacets = mergeFacets(allFacets);

        // enrich node facet with pid and create page
        List<Node> nodes = nodeResolver.fetchNodes();
        enrichNodeFacet(mergedFacets, nodes);

        return createPage(from, finalResults.size(), totalAvailable, finalResults, mergedFacets, nodes);
    }

    private Optional<APIPageMetadata> fetchPageMetadata(String endpoint, FacetFilter ff) {
        String url = buildUrlWithFacetFilter(endpoint, ff, 0, 0);

        return fetchPage(url, FetchPhase.METADATA)
                .map(page -> new APIPageMetadata(
                        endpoint,
                        page.getTotal(),
                        Optional.ofNullable(page.getFacets()).orElse(Collections.emptyList())
                ));
    }

    private Optional<Paging<?>> fetchResultsPage(String url) {
        return fetchPage(url, FetchPhase.DATA);
    }

    private Optional<Paging<?>> fetchPage(String url, FetchPhase phase) {
        try {
            Paging<?> page = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Paging.class);

            return Optional.ofNullable(page);
        } catch (Exception e) {
            logger.warn("Skipping unavailable node during {} fetch: {} ({})",
                    phase.logLabel, url, describeException(e));
            logger.debug("Unavailable node details for {}", url, e);
            return Optional.empty();
        }
    }

    private String describeException(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + message;
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

    private void enrichNodeFacet(List<Facet> facets, List<Node> nodes) {
        Set<String> knownPids = nodes.stream()
                .map(Node::pid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        facets.stream()
                .filter(f -> "node".equals(f.getField()))
                .findFirst()
                .ifPresent(nodeFacet -> {
                    List<Value> enriched = nodeFacet.getValues().stream()
                            .map(v -> new NodeFacetValue(v, knownPids.contains(v.getValue()) ? v.getValue() : FALLBACK_PID))
                            .collect(Collectors.toList());
                    nodeFacet.setValues(enriched);
                });
    }

    private Page<Object> createPage(int from, int resultsSize, int total, List<Object> results, List<Facet> facets, List<Node> nodes) {
        Page<Object> page = new Page<>(total, from, from + resultsSize, results, facets);
        page.setMetadata(Map.of("nodes", nodes));
        return page;
    }

    private static class APIPageMetadata {
        String url;
        int size;
        List<Facet> facets;

        APIPageMetadata(String url, int size, List<Facet> facets) {
            this.url = url;
            this.size = size;
            this.facets = facets;
        }
    }

    private enum FetchPhase {
        METADATA("metadata"),
        DATA("data");

        private final String logLabel;

        FetchPhase(String logLabel) {
            this.logLabel = logLabel;
        }
    }
}
