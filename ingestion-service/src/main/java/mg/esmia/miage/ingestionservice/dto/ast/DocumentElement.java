package mg.esmia.miage.ingestionservice.dto.ast;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DocumentElement(
    String id,
    ElementType type,
    Integer level,
    String text,
    List<Float> bbox,
    int page,
    @JsonProperty("parent_id") String parentId,
    double confidence,
    @JsonProperty("table_data") TableData tableData
) {}
