package com.example.bagservice.service;

import com.example.bagservice.domain.BagItem;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The only service in the backend: it returns the hardcoded contents of a customer's bag.
 *
 * <p>This list is simply what this branch of the code returns. To ship a bag with different
 * contents, change the list on a branch and deploy that branch as its own version — the pipeline
 * builds the branch name as the version, so branch {@code feature1} becomes version
 * {@code feature1} and runs alongside the current one. Two versions differ because their code
 * differs, which is the only reason two versions of anything ever differ.</p>
 *
 * <p>The injected version is reported, never consulted: it identifies which build answered a
 * request. Nothing here inspects it, and nothing here inspects the routing cookies — which
 * version of this service receives the request is decided by the mesh, before the request
 * arrives.</p>
 */
@Service
public class BagService {

    private static final List<BagItem> ITEMS = List.of(
            new BagItem("itm-001", "TOTE-CLS-BLK", "Classic Leather Tote", "Black", "Large", 1, 189.00),
            new BagItem("itm-002", "CROSS-LTH-TAN", "Leather Crossbody", "Tan", "Small", 1, 129.50),
            new BagItem("itm-003", "BPK-CNV-NVY", "Canvas Backpack", "Navy", "Medium", 2, 74.25),
            new BagItem("itm-004", "DUF-WKD-OLV", "Weekender Duffel", "Olive", "Large", 1, 215.00));

    private final String version;

    public BagService(@Value("${bag.version}") String version) {
        this.version = version;
    }

    /** The version of this build, for reporting only. */
    public String version() {
        return version;
    }

    public List<BagItem> items() {
        return ITEMS;
    }

    public double subtotal() {
        double total = ITEMS.stream().mapToDouble(BagItem::lineTotal).sum();
        return Math.round(total * 100.0) / 100.0;
    }

    public int itemCount() {
        return ITEMS.stream().mapToInt(BagItem::quantity).sum();
    }
}
