package gr.uoa.di.madgik.federation.search.aggregator.controller;

import gr.uoa.di.madgik.federation.search.aggregator.dto.NodeInfo;
import gr.uoa.di.madgik.federation.search.aggregator.service.NodeResolver;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "nodes", produces = {MediaType.APPLICATION_JSON_VALUE})
public class NodesController {

    private final NodeResolver nodeResolver;

    public NodesController(NodeResolver nodeResolver) {
        this.nodeResolver = nodeResolver;
    }

    @Operation(summary = "Get all Nodes from the Node Registry.")
    @GetMapping
    public ResponseEntity<List<NodeInfo>> getNodes() {
        return ResponseEntity.ok(nodeResolver.fetchNodes().stream()
                .map(n -> new NodeInfo(n.getPid(), n.getName(), n.getLogo(), n.getNodeEndpoint()))
                .toList());
    }
}
