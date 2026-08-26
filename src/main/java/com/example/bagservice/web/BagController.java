package com.example.bagservice.web;

import com.example.bagservice.routing.RoutingContext;
import com.example.bagservice.routing.RoutingContextHolder;
import com.example.bagservice.service.BagService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The only controller in the backend. */
@RestController
public class BagController {

    private final BagService bagService;
    private final String layer;
    private final String instance;

    public BagController(BagService bagService,
                         @Value("${bag.layer}") String layer,
                         @Value("${bag.instance}") String instance) {
        this.bagService = bagService;
        this.layer = layer;
        this.instance = instance;
    }

    /**
     * Returns the hardcoded contents of the bag, plus the routing context this pod received.
     *
     * <p>Echoing the routing context back is the propagation proof: if {@code bag_fed},
     * {@code bag_orch} and {@code bag_service} arrive here intact, every layer above forwarded
     * them correctly — this is the last hop in the chain.</p>
     */
    @GetMapping("/api/bags")
    public Map<String, Object> getBag() {
        RoutingContext routing = RoutingContextHolder.get();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layer", layer);
        body.put("version", bagService.version());
        body.put("instance", instance);
        body.put("servedAt", Instant.now().toString());
        body.put("routingContextReceived", routing.asReportedMap());
        body.put("currency", "USD");
        body.put("itemCount", bagService.itemCount());
        body.put("subtotal", bagService.subtotal());
        body.put("items", bagService.items());
        return body;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("layer", layer);
        body.put("version", bagService.version());
        body.put("instance", instance);
        return body;
    }
}
