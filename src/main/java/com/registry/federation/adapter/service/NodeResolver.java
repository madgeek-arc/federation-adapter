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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.registry.federation.adapter.model.Node;
import com.registry.federation.adapter.model.Capability;
import com.registry.federation.adapter.model.NodeCapabilitiesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Service
public class NodeResolver {

    private static final Logger logger = LoggerFactory.getLogger(NodeResolver.class);

    private String nodeRegistryUrl;
    private String nodeRegistryKey;

    private RestClient webClient;

    public NodeResolver(@Value("${node.registry.url}") String nodeRegistryUrl,
                        @Value("${node.registry.key}") String nodeRegistryKey) {
        this.webClient = RestClient.builder()
                .baseUrl(nodeRegistryUrl)
                .build();
        this.nodeRegistryKey = nodeRegistryKey;
    }

    @Cacheable(cacheNames = "nodes", unless = "#result == null || #result.isEmpty()")
    public List<Node> fetchNodes() {
        List<Node> nodes = (List<Node>) webClient.get()
                .header("x-api-key", nodeRegistryKey)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Node>>() {});
        if (nodes == null) {
            return List.of();
        }

        return nodes.stream()
                .map(this::populateCapabilities)
                .toList();
    }

    private Node populateCapabilities(Node node) {
        List<Capability> capabilities = fetchCapabilities(node.nodeEndpoint());
        return new Node(
                node.id(),
                node.name(),
                node.logo(),
                node.pid(),
                node.legalEntity(),
                node.nodeEndpoint(),
                capabilities
        );
    }

    private List<Capability> fetchCapabilities(URI nodeEndpoint) {
        try {
            NodeCapabilitiesResponse response = webClient.mutate()
                    .baseUrl(UriComponentsBuilder.fromUri(nodeEndpoint).build().toUriString())
                    .build()
                    .get()
                    .retrieve()
                    .body(NodeCapabilitiesResponse.class);

            if (response == null || response.capabilities() == null) {
                return List.of();
            }

            return response.capabilities();
        } catch (Exception e) {
            logger.warn("Failed to fetch capabilities from node endpoint {}", nodeEndpoint, e);
            return Collections.emptyList();
        }
    }
}
