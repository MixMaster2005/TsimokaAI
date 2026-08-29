package mg.esmia.miage.ingestionservice.dto.ast;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ElementType {
    HEADING, PARAGRAPH, LIST, TABLE, FIGURE, CAPTION, CODE, QUOTE;

    @JsonCreator
    public static ElementType fromValue(String value) {
        if (value == null) return null;
        for (ElementType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("ElementType inconnu : " + value);
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
