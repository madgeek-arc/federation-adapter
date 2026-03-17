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

package com.registry.federation.adapter.model;

import java.util.List;

public class NodeCapabilityResponse {

    private String node_endpoint;
    private List<Capability> capabilities;

    public String getNode_endpoint() {
        return node_endpoint;
    }

    public void setNode_endpoint(String node_endpoint) {
        this.node_endpoint = node_endpoint;
    }

    public List<Capability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public static class Capability {
        private String capability_type;
        private String endpoint;
        private String version;

        public String getCapability_type() {
            return capability_type;
        }

        public void setCapability_type(String capability_type) {
            this.capability_type = capability_type;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
