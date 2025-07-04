package com.registry.federation.adapter.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ExternalConfigLoader {

    @Bean
    public NodeProperties nodeProperties(ObjectMapper objectMapper) throws IOException {
        Resource resource = new ClassPathResource("node-endpoints.json");
        JsonNode jsonNode = objectMapper.readTree(resource.getInputStream());

        NodeProperties nodeProperties = new NodeProperties();
        List<String> endpoints = new ArrayList<>();

        if (jsonNode.has("endpoints")) {
            for (JsonNode endpointNode : jsonNode.get("endpoints")) {
                endpoints.add(endpointNode.asText());
            }
        }

        nodeProperties.setEndpoints(endpoints);
        return nodeProperties;
    }
}
