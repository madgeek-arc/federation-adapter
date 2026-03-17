package com.registry.federation.adapter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registry.federation.adapter.model.NodeCapabilityResponse;
import com.registry.federation.adapter.model.NodeRegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class NodeEndpointService {

    private static final Logger logger = LoggerFactory.getLogger(NodeEndpointService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final boolean manualConfig;

    private volatile List<String> cachedEndpoints = new ArrayList<>();

    public NodeEndpointService(WebClient webClient,
                               ObjectMapper objectMapper,
                               @Value("${node.endpoints.manual-config}") boolean manualConfig,
                               @Value("${node.endpoints.url}") String apiUrl,
                               @Value("${node.endpoints.key}") String apiKey) {

        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.manualConfig = manualConfig;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;

        if (manualConfig) {
            try {
                cachedEndpoints = loadFromJson(objectMapper);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load manual node-endpoints.json", e);
            }
        } else {
            cachedEndpoints = loadFromApi();
        }
    }

    public List<String> getCurrentEndpoints() {
        return Collections.unmodifiableList(cachedEndpoints);
    }

    @Scheduled(fixedRateString = "PT1H")
    public void refreshEndpoints() {
        if (!manualConfig) {
            try {
                cachedEndpoints = loadFromApi();
                logger.info("Node endpoints refreshed: {}", cachedEndpoints);
            } catch (Exception e) {
                logger.error("Failed to refresh endpoints: {}", e.getMessage());
            }
        }
    }

    private List<String> loadFromJson(ObjectMapper objectMapper) throws IOException {
        Resource resource = new ClassPathResource("node-endpoints.json");
        JsonNode jsonNode = objectMapper.readTree(resource.getInputStream());

        List<String> endpoints = new ArrayList<>();
        if (jsonNode.has("endpoints")) {
            for (JsonNode endpointNode : jsonNode.get("endpoints")) {
                endpoints.add(endpointNode.asText());
            }
        }
        return endpoints;
    }

    private List<String> loadFromApi() {
        try {
            // get all node endpoints registered on the federation
            List<NodeRegistryEntry> nodes = webClient.get()
                    .uri(apiUrl)
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .bodyToFlux(NodeRegistryEntry.class)
                    .collectList()
                    .block();

            List<String> finalEndpoints = new ArrayList<>();
            if (nodes != null) {
                for (NodeRegistryEntry node : nodes) {
                    String nodeEndpointUrl = node.getNode_endpoint();
                    if (nodeEndpointUrl == null || nodeEndpointUrl.isEmpty()) continue;
                    try {
                        // get endpoint's capabilities
                        NodeCapabilityResponse response = webClient.get()
                                .uri(nodeEndpointUrl)
                                .retrieve()
                                .bodyToMono(NodeCapabilityResponse.class)
                                .timeout(Duration.ofSeconds(5))
                                .onErrorResume(e -> Mono.empty())
                                .block();
                        // check for Resource Catalogue capability
                        if (response != null && response.getCapabilities() != null) {
                            response.getCapabilities().stream()
                                    .filter(cap -> "Resource Catalogue".equalsIgnoreCase(cap.getCapability_type()))
                                    .filter(cap -> isValid(cap.getEndpoint()) && isValid(cap.getVersion()))
                                    .findFirst()
                                    .ifPresent(cap -> {
                                        // create proper API calls
                                        String rcEndpoint = cap.getEndpoint();
                                        String fullEndpoint = rcEndpoint.endsWith("/")
                                                ? rcEndpoint + "public/service/search"
                                                : rcEndpoint + "/public/service/search";
                                        finalEndpoints.add(fullEndpoint);
                                    });
                        }
                    } catch (Exception ex) {
                        logger.info("Skipping failing node: {}", nodeEndpointUrl);
                    }
                }
            }
            return finalEndpoints;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch nodes from federation API", e);
        }
    }

    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty() && !value.equals("-");
    }
}