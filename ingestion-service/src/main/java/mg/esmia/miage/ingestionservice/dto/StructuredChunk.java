package mg.esmia.miage.ingestionservice.dto;

import mg.esmia.miage.ingestionservice.dto.ast.ElementType;

import java.util.List;
import java.util.Set;

/**
 * Chunk structuré produit par le {@link mg.esmia.miage.ingestionservice.service.StructureAwareChunker}.
 *
 * <p>Chaque chunk conserve son contexte structurel (chemin de titres, pages, types d'éléments)
 * pour permettre des citations précises et un retrieval enrichi.</p>
 *
 * @param chunkIndex   index du chunk dans le document (0-based)
 * @param text         texte du chunk (pour embedding et stockage Qdrant)
 * @param headingPath  chemin hiérarchique des titres parent (ex: ["Chapitre 1", "1.2 Architecture"])
 * @param pageStart    première page du chunk
 * @param pageEnd      dernière page du chunk
 * @param elementTypes types d'éléments AST contenus dans le chunk
 * @param imageIds     identifiants des images associées (V1: toujours vide, prêt pour V2)
 */
public record StructuredChunk(
    int chunkIndex,
    String text,
    List<String> headingPath,
    int pageStart,
    int pageEnd,
    Set<ElementType> elementTypes,
    List<String> imageIds
) {}
