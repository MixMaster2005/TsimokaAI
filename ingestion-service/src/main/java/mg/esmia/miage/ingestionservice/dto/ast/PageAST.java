package mg.esmia.miage.ingestionservice.dto.ast;

import java.util.List;

public record PageAST(
    int page,
    List<DocumentElement> elements
) {}
