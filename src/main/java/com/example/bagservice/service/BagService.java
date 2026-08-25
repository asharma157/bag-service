package com.example.bagservice.service;

import com.example.bagservice.domain.BagItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The only service in the backend: it returns the hardcoded contents of a customer's bag.
 *
 * <p>Each shipped version of bag-service returns a visibly different item list, which is what
 * makes cookie routing verifiable by eye. In a real pipeline the list would simply differ between
 * source revisions and each revision would be its own image. Here every version's list lives in
 * one source tree, keyed by version, so a single image can be deployed as 1.9, 1.10 or feature1
 * and the POC stays cheap to run. The pod's {@code version} label (via APP_VERSION) is what picks
 * the list — nothing in the request body or the routing cookies is ever consulted.</p>
 */
@Service
public class BagService {

    private static final String FALLBACK_VERSION = "1.9";

    private final Map<String, List<BagItem>> catalogueByVersion = buildCatalogues();
    private final String version;

    public BagService(@Value("${bag.version}") String version) {
        this.version = version;
    }

    /** The version this instance reports and serves. */
    public String version() {
        return version;
    }

    /**
     * The version whose item list is actually being served. Normally identical to {@link #version()};
     * they differ only if a pod is labelled with a version that has no catalogue of its own, in
     * which case the production list is served and the mismatch is visible in the response.
     */
    public String catalogueVersion() {
        return catalogueByVersion.containsKey(version) ? version : FALLBACK_VERSION;
    }

    public List<BagItem> items() {
        return catalogueByVersion.getOrDefault(version, catalogueByVersion.get(FALLBACK_VERSION));
    }

    public double subtotal() {
        double total = items().stream().mapToDouble(BagItem::lineTotal).sum();
        return Math.round(total * 100.0) / 100.0;
    }

    public int itemCount() {
        return items().stream().mapToInt(BagItem::quantity).sum();
    }

    private static Map<String, List<BagItem>> buildCatalogues() {
        Map<String, List<BagItem>> catalogues = new LinkedHashMap<>();

        // 1.9 — the version currently taking 100% of default traffic.
        catalogues.put("1.9", List.of(
                new BagItem("itm-001", "TOTE-CLS-BLK", "Classic Leather Tote", "Black", "Large", 1, 189.00),
                new BagItem("itm-002", "CROSS-LTH-TAN", "Leather Crossbody", "Tan", "Small", 1, 129.50),
                new BagItem("itm-003", "BPK-CNV-NVY", "Canvas Backpack", "Navy", "Medium", 2, 74.25)));

        // 1.10 — a developer's next release: one extra line item and a repriced tote.
        catalogues.put("1.10", List.of(
                new BagItem("itm-001", "TOTE-CLS-BLK", "Classic Leather Tote", "Black", "Large", 1, 169.00),
                new BagItem("itm-002", "CROSS-LTH-TAN", "Leather Crossbody", "Tan", "Small", 1, 129.50),
                new BagItem("itm-003", "BPK-CNV-NVY", "Canvas Backpack", "Navy", "Medium", 2, 74.25),
                new BagItem("itm-004", "DUF-WKD-OLV", "Weekender Duffel", "Olive", "Large", 1, 215.00)));

        // feature1 — a branch build; deliberately a short, obviously different list.
        catalogues.put("feature1", List.of(
                new BagItem("itm-101", "TOTE-MON-BLK", "Monogrammed Tote (feature1)", "Black", "Large", 1, 199.00),
                new BagItem("itm-102", "POUCH-GFT-RED", "Gift Pouch (feature1)", "Red", "One Size", 3, 19.00)));

        return catalogues;
    }
}
