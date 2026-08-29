package mg.esmia.miage.ingestionservice.dto.ast;

import java.util.List;

public record CanonicalDocument(
    List<PageAST> pages,
    List<ImageRef> images
) {}
