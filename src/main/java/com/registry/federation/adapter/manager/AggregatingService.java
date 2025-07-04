package com.registry.federation.adapter.manager;

import com.registry.federation.adapter.configuration.NodeProperties;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class AggregatingService {

    @Autowired
    private NodeProperties nodeProperties;

    private final RestTemplate restTemplate;

    public AggregatingService(NodeProperties nodeProperties) {
        this.restTemplate = new RestTemplate();
    }

    public Paging<Object> getMergedPagedResults(FacetFilter ff) {
        int from = ff.getFrom();
        int quantity = ff.getQuantity();
        int to = from + quantity;

        List<String> urls = nodeProperties.getEndpoints();
        List<Object> allResults = new ArrayList<>();
        int totalAvailable = 0;

        for (String url : urls) {
            if (allResults.size() >= to) break;

//            String fullUrl = url + "?from=0&to=10000"; // or actual pagination if possible

            try {
                ResponseEntity<Paging> response = restTemplate.getForEntity(url, Paging.class);
                Paging<Object> page = response.getBody();
                if (page == null || page.getResults() == null) continue;

                totalAvailable += page.getTotal();
                allResults.addAll(page.getResults());

            } catch (Exception e) {
                // log error
            }
        }

        // Slice results
        int sliceFrom = Math.min(from, allResults.size());
        int sliceTo = Math.min(to, allResults.size());
        List<Object> resultSlice = allResults.subList(sliceFrom, sliceTo);

        Paging<Object> paging = new Paging<>();
        paging.setFrom(from);
        paging.setTo(sliceFrom + resultSlice.size());
        paging.setTotal(totalAvailable);
        paging.setResults(resultSlice);

        return paging;
    }

}

