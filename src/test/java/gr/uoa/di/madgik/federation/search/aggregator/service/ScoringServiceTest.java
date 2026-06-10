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

import gr.uoa.di.madgik.federation.search.aggregator.dto.AggregatedResult;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoringServiceTest {

    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        ScoringService.RrfProperties properties = new ScoringService.RrfProperties();
        properties.setK(60);
        scoringService = new ScoringService(properties);
    }

    @Test
    void testApplyRRF() {
        // Node 1: A1 (#1), A2 (#2)
        HighlightedResult resA1 = mock(HighlightedResult.class);
        when(resA1.getScore()).thenReturn(10.0f);
        when(resA1.getResult()).thenReturn(new HashMap<>(Map.of("id", "A1")));

        HighlightedResult resA2 = mock(HighlightedResult.class);
        when(resA2.getScore()).thenReturn(9.0f);
        when(resA2.getResult()).thenReturn(new HashMap<>(Map.of("id", "A2")));

        // Node 2: B1 (#1), A1 (#2)
        HighlightedResult resB1 = mock(HighlightedResult.class);
        when(resB1.getScore()).thenReturn(10.0f);
        when(resB1.getResult()).thenReturn(new HashMap<>(Map.of("id", "B1")));

        HighlightedResult resA1_again = mock(HighlightedResult.class);
        when(resA1_again.getScore()).thenReturn(8.0f);
        when(resA1_again.getResult()).thenReturn(new HashMap<>(Map.of("id", "A1")));

        Map<String, List<HighlightedResult>> nodeResults = new HashMap<>();
        nodeResults.put("node1", List.of(resA1, resA2));
        nodeResults.put("node2", List.of(resB1, resA1_again));

        List<AggregatedResult> sortedResults = scoringService.applyRRF(nodeResults);

        assertThat(sortedResults).hasSize(3);

        // A1 should be first (sum of ranks)
        AggregatedResult first = sortedResults.get(0);
        assertThat(scoringService.extractId(first)).isEqualTo("A1");
        assertThat(first.score()).isCloseTo(0.03252, org.assertj.core.data.Offset.offset(0.00001));

        // B1 and A2 are tied in RRF (both #1 or #2 in one node)
        // Wait, B1 is #1 in node2, A2 is #2 in node1.
        // B1 score: 1/(60+1) = 0.01639
        // A2 score: 1/(60+2) = 0.01612
        // So B1 should be second.
        AggregatedResult second = sortedResults.get(1);
        assertThat(scoringService.extractId(second)).isEqualTo("B1");

        AggregatedResult third = sortedResults.get(2);
        assertThat(scoringService.extractId(third)).isEqualTo("A2");
    }

    @Test
    void testTieBreaking() {
        // A1 (#1 in node1, score 10)
        HighlightedResult resA1 = mock(HighlightedResult.class);
        when(resA1.getScore()).thenReturn(10.0f);
        when(resA1.getResult()).thenReturn(new HashMap<>(Map.of("id", "A1")));

        // B1 (#1 in node2, score 8)
        HighlightedResult resB1 = mock(HighlightedResult.class);
        when(resB1.getScore()).thenReturn(8.0f);
        when(resB1.getResult()).thenReturn(new HashMap<>(Map.of("id", "B1")));

        Map<String, List<HighlightedResult>> nodeResults = new HashMap<>();
        nodeResults.put("node1", List.of(resA1));
        nodeResults.put("node2", List.of(resB1));

        List<AggregatedResult> sortedResults = scoringService.applyRRF(nodeResults);

        // Both have RRF score 1/(60+1). A1 wins tie-break with 10.0 > 8.0
        assertThat(scoringService.extractId(sortedResults.get(0))).isEqualTo("A1");
        assertThat(scoringService.extractId(sortedResults.get(1))).isEqualTo("B1");
    }
}
