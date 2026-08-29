package mg.esmia.miage.ingestionservice.dto.ast;

import java.util.List;

public record TableData(
    List<String> headers,
    List<List<String>> rows
) {}
