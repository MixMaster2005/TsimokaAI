package mg.esmia.miage.ingestionservice.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Source unique de vérité pour les formats de documents supportés par le système.
 * Utilisé par le controller (validation HTTP), le service (validation métier) et
 * le worker (routing par extension).
 *
 * <p>Ajouter un nouveau format = ajouter un enum + mapping markitdown côté worker.</p>
 */
public enum SupportedDocumentType {

    PDF("pdf", "PDF", "application/pdf", "application/x-pdf"),
    DOCX("docx", "Word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    PPTX("pptx", "PowerPoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    XLSX("xlsx", "Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    XLS("xls", "Excel", "application/vnd.ms-excel", "application/excel"),
    CSV("csv", "CSV", "text/csv", "application/csv"),
    TXT("txt", "Texte", "text/plain"),
    MARKDOWN("md", "Markdown", "text/markdown", "text/x-markdown"),
    HTML("html", "HTML", "text/html", "application/xhtml+xml"),
    EPUB("epub", "EPUB", "application/epub+zip", "application/epub");

    private final String extension;
    private final String displayName;
    private final String[] mimeTypes;

    /** Index : extension (minuscule, sans point) → enum */
    private static final Map<String, SupportedDocumentType> BY_EXTENSION =
            Arrays.stream(values()).collect(Collectors.toMap(SupportedDocumentType::getExtension, Function.identity()));

    /** Index : MIME type (minuscule) → enum */
    private static final Map<String, SupportedDocumentType> BY_MIME = buildMimeIndex();

    private static Map<String, SupportedDocumentType> buildMimeIndex() {
        Map<String, SupportedDocumentType> map = new HashMap<>();
        for (SupportedDocumentType type : values()) {
            for (String mime : type.mimeTypes) {
                map.put(mime, type);
            }
        }
        return map;
    }

    SupportedDocumentType(String extension, String displayName, String... mimeTypes) {
        this.extension = extension;
        this.displayName = displayName;
        this.mimeTypes = mimeTypes;
    }

    public String getExtension() {
        return extension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Recherche par extension (minuscule, sans point).
     * Exemples : "pdf", "docx", "md"
     */
    public static SupportedDocumentType fromExtension(String extension) {
        if (extension == null) return null;
        return BY_EXTENSION.get(extension.toLowerCase(Locale.ROOT));
    }

    /**
     * Recherche par type MIME.
     * Exemples : "application/pdf", "text/plain"
     */
    public static SupportedDocumentType fromMimeType(String mimeType) {
        if (mimeType == null) return null;
        return BY_MIME.get(mimeType.toLowerCase(Locale.ROOT));
    }

    /**
     * Vérifie si une extension est supportée.
     */
    public static boolean isSupportedExtension(String extension) {
        return fromExtension(extension) != null;
    }

    /**
     * Vérifie si un MIME type est supporté.
     */
    public static boolean isSupportedMimeType(String mimeType) {
        return fromMimeType(mimeType) != null;
    }

    /**
     * Message listant tous les formats supportés, pour les erreurs utilisateur.
     */
    public static String supportedFormatsMessage() {
        return "Formats acceptés : " + Arrays.stream(values())
                .map(SupportedDocumentType::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Retourne l'extension canonique pour un nom de fichier donné.
     * Si le fichier a une extension supportée, retourne l'extension canonique (minuscule).
     * Sinon retourne null.
     */
    public static String canonicalExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return null;
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return BY_EXTENSION.containsKey(ext) ? ext : null;
    }
}
