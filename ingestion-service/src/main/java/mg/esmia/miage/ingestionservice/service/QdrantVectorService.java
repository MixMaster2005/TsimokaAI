package mg.esmia.miage.ingestionservice.service;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.ingestionservice.dto.StructuredChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Accès à la base vectorielle Qdrant — **pattern multi-tenant : une seule collection partagée**
 * (nom configurable, défaut {@code chunks}), chaque point portant son {@code space_id} en
 * payload (Option A). Le cloisonnement par espace se fait donc par **filtre** sur le payload
 * (ex. {@code filterExpression} de {@code QuestionAnswerAdvisor} / Spring AI), et plus par le
 * nom de la collection. Metadata par point :
 * {@code {document_id, space_id, chunk_index, content, page_start, page_end,
 * heading_path, element_types}}.
 *
 * <p>Appels gRPC bloquants via {@code ListenableFuture.get(timeout)} ; les interactions sont
 * validées par un smoke test contre un vrai conteneur Qdrant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantVectorService {

    /** Timeout des appels Qdrant (gRPC bloquant via ListenableFuture.get). */
    private static final int QDRANT_TIMEOUT_SECONDS = 30;

    private final QdrantClient qdrantClient;

    @Value("${qdrant.collection-name:chunks}")
    private String collectionName;

    /** Nom de la collection vectorielle unique (tous les espaces y partagent leurs chunks). */
    public String collectionName() {
        return collectionName;
    }

    /** Crée la collection unique si elle n'existe pas encore (dimensions déduites des embeddings
     *  réels, distance Cosine). */
    public void ensureCollection(long dimensions) {
        try {
            qdrantClient.getCollectionInfoAsync(collectionName)
                    .get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Collection absente (QdrantException NOT_FOUND) ou erreur réseau -> on tente la création.
            try {
                qdrantClient.createCollectionAsync(collectionName,
                                VectorParams.newBuilder()
                                        .setSize(dimensions)
                                        .setDistance(Distance.Cosine)
                                        .build())
                        .get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.info("Collection Qdrant '{}' créée ({} dimensions)", collectionName, dimensions);
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Impossible de créer la collection Qdrant '" + collectionName + "' : " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Upsert des vecteurs d'un document dans la collection unique. Chaque point porte son
     * {@code space_id} et les métadonnées structurées en payload (filtré côté retrieval).
     *
     * @return les identifiants UUID des points (persistés dans {@code Chunk.vectorId}).
     */
    public List<UUID> upsertChunks(UUID documentId, UUID spaceId,
                                   List<StructuredChunk> chunks, List<float[]> vectors) {
        String documentIdStr = documentId.toString();
        String spaceIdStr = spaceId.toString();
        List<PointStruct> points = new ArrayList<>();
        List<UUID> vectorIds = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            StructuredChunk chunk = chunks.get(i);
            UUID pointId = UUID.randomUUID();
            vectorIds.add(pointId);
            points.add(PointStruct.newBuilder()
                    .setId(PointIdFactory.id(pointId))
                    .setVectors(VectorsFactory.vectors(vectors.get(i)))
                    .putPayload("document_id", ValueFactory.value(documentIdStr))
                    .putPayload("space_id", ValueFactory.value(spaceIdStr))
                    .putPayload("chunk_index", ValueFactory.value(chunk.chunkIndex()))
                    .putPayload("doc_content", ValueFactory.value(chunk.text()))
                    .putPayload("content", ValueFactory.value(chunk.text()))
                    .putPayload("page_start", ValueFactory.value(chunk.pageStart()))
                    .putPayload("page_end", ValueFactory.value(chunk.pageEnd()))
                    .putPayload("heading_path", ValueFactory.value(
                            String.join(" > ", chunk.headingPath())))
                    .putPayload("element_types", ValueFactory.value(
                            chunk.elementTypes().stream().map(Enum::name)
                                    .collect(Collectors.joining(","))))
                    .putPayload("image_ids", ValueFactory.value(
                            String.join(",", chunk.imageIds())))
                    .build());
        }
        try {
            qdrantClient.upsertAsync(collectionName, points)
                    .get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Upsert Qdrant échoué ({} points) : {}".formatted(points.size(), e.getMessage()), e);
        }
        log.info("Upsert de {} points dans Qdrant ({})", points.size(), collectionName);
        return vectorIds;
    }

    /** Supprime les points Qdrant d'un document (filtre par document_id), non bloquant en échec. */
    public void deletePoints(UUID documentId) {
        Filter filter = Filter.newBuilder()
                .addAllMust(List.of(ConditionFactory.matchKeyword("document_id", documentId.toString())))
                .build();
        try {
            qdrantClient.deleteAsync(collectionName, filter)
                    .get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Purge Qdrant impossible pour le document {} (ignoré) : {}",
                    documentId, e.getMessage());
        }
    }
}
