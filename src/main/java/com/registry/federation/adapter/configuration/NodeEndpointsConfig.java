/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package com.registry.federation.adapter.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registry.federation.adapter.model.NodeCapabilityResponse;
import com.registry.federation.adapter.model.NodeProperties;
import com.registry.federation.adapter.model.NodeRegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class NodeEndpointsConfig {

    private static final Logger logger = LoggerFactory.getLogger(NodeEndpointsConfig.class);

    @Value("${node.endpoints.manual-config}")
    private boolean manualConfig;
    @Value("${node.endpoints.url}")
    private String url;
    @Value("${node.endpoints.x-api-key}")
    private String apiKey;

    private final WebClient webClient;

    public NodeEndpointsConfig(WebClient webClient) {
        this.webClient = webClient;
    }

    @Bean
    public NodeProperties nodeProperties(ObjectMapper objectMapper) throws IOException {
        List<String> endpoints;

        if (manualConfig) {
            endpoints = loadFromJson(objectMapper);
        } else {
            endpoints = loadFromApi();
        }

        NodeProperties nodeProperties = new NodeProperties();
        nodeProperties.setEndpoints(endpoints);
        return nodeProperties;
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
                    .uri(url)
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
                        System.out.println("Skipping failing node: " + nodeEndpointUrl);
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
