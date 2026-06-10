package gr.uoa.di.madgik.federation.search.aggregator.service;

import gr.uoa.di.madgik.federation.search.aggregator.dto.Page;
import gr.uoa.di.madgik.federation.search.aggregator.dto.AggregatedResult;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AggregatingServiceTest {

    private AggregatingService aggregatingService;
    private RestClient restClient;
    private NodeEndpointService nodeEndpointService;
    private NodeResolver nodeResolver;
    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        nodeEndpointService = mock(NodeEndpointService.class);
        nodeResolver = mock(NodeResolver.class);
        
        ScoringService.RrfProperties properties = new ScoringService.RrfProperties();
        properties.setK(60);
        scoringService = new ScoringService(properties);
        
        aggregatingService = new AggregatingService(restClient, nodeEndpointService, nodeResolver, scoringService);
    }

    @Test
    void testRRFMerging() {
        // Prepare endpoints
        when(nodeEndpointService.getResourceCatalogueEndpoints()).thenReturn(List.of("node1", "node2"));
        when(nodeResolver.fetchNodes()).thenReturn(Collections.emptyList());

        // Prepare Mock RestClient behavior
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.accept(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        // Node 1 results
        HighlightedResult resA1 = mock(HighlightedResult.class);
        when(resA1.getScore()).thenReturn(10.0f);
        when(resA1.getResult()).thenReturn(new HashMap<>(Map.of("id", "A1")));

        HighlightedResult resA2 = mock(HighlightedResult.class);
        when(resA2.getScore()).thenReturn(9.0f);
        when(resA2.getResult()).thenReturn(new HashMap<>(Map.of("id", "A2")));

        Paging<HighlightedResult> paging1 = new Paging<>(2, 0, 2, List.of(resA1, resA2), Collections.emptyList());

        // Node 2 results
        HighlightedResult resB1 = mock(HighlightedResult.class);
        when(resB1.getScore()).thenReturn(10.0f);
        when(resB1.getResult()).thenReturn(new HashMap<>(Map.of("id", "B1")));

        HighlightedResult resA1_again = mock(HighlightedResult.class);
        when(resA1_again.getScore()).thenReturn(8.0f);
        when(resA1_again.getResult()).thenReturn(new HashMap<>(Map.of("id", "A1")));

        Paging<HighlightedResult> paging2 = new Paging<>(2, 0, 2, List.of(resB1, resA1_again), Collections.emptyList());

        // First call is for metadata, returns size 2
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(paging1) // node1 metadata
                .thenReturn(paging2) // node2 metadata
                .thenReturn(paging1) // node1 data
                .thenReturn(paging2); // node2 data

        FacetFilter ff = new FacetFilter();
        ff.setFrom(0);
        ff.setQuantity(10);

        Page<AggregatedResult> resultPage = aggregatingService.getMergedPagedResults(ff, "service");

        assertThat(resultPage.getTotal()).isEqualTo(4);
        List<AggregatedResult> results = resultPage.getResults();
        assertThat(results).hasSize(3); // A1, A2, B1

        // A1 should be first because it's in both nodes
        // Score A1: 1/(60+1) [from node1] + 1/(60+2) [from node2] = 0.01639 + 0.01612 = 0.03251
        // Score B1: 1/(60+1) = 0.01639
        // Score A2: 1/(60+2) = 0.01612

        AggregatedResult first = results.get(0);
        assertThat(first.result().get("id")).isEqualTo("A1");
        // A1 RRF score is 0.03252.
        assertThat(first.score()).isCloseTo(0.03252, org.assertj.core.data.Offset.offset(0.00001));
        // originalScore is the score from the node where it was first found (node1: 10.0)
        assertThat(first.originalScore()).isEqualTo(10.0);

        AggregatedResult second = results.get(1);
        assertThat(second.result().get("id")).isEqualTo("B1");

        AggregatedResult third = results.get(2);
        assertThat(third.result().get("id")).isEqualTo("A2");
    }
}
