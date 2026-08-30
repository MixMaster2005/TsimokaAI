package mg.esmia.miage.ingestionservice.dto;

import java.util.Map;

public record ImageResolveResponse(Map<String, ResolvedImage> images) {
    public record ResolvedImage(String url, String caption) {}
}
