package mg.esmia.miage.ingestionservice.dto.ast;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ImageRef(
    @JsonProperty("placeholder_id") String placeholderId,
    @JsonProperty("content_type") String contentType,
    @JsonProperty("data_base64") String dataBase64,
    String caption,
    List<Float> bbox,
    int page
) {}
