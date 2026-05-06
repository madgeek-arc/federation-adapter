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

package gr.uoa.di.madgik.federation.search.aggregator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import gr.uoa.di.madgik.node.capabilities.model.Capability;

import java.net.URI;
import java.util.List;

public record Node(
        String id,
        String name,
        URI logo,
        String pid,
        @JsonProperty("legal_entity")
        LegalEntity legalEntity,
        @JsonProperty("node_endpoint")
        URI nodeEndpoint,
        List<Capability> capabilities) {

    public record LegalEntity(String name, @JsonProperty("ror_id") String rorId) {}
}
