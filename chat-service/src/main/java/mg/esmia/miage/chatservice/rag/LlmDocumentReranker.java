package mg.esmia.miage.chatservice.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.aicommon.ChatProviderResolver;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reranking par LLM (provider actif, typiquement Groq — rapide) : le modèle reçoit les
 * candidats tagués {@code [C0]..[Cn]} et retourne leur ordre de pertinence ; on réordonne
 * et on garde le {@code topN}.
 *
 * <p>Stratégie retenue (cf. ARCHITECTURE.md §6.3 — « RerankingDocumentPostProcessor ») :
 * <b>zéro nouvelle dépendance</b> (pas de provider de reranking dédié type Cohere/Jina à
 * gérer, même arbitrage que le reste du projet : éviter les services externes additionnels).
 * Le coût d'un appel LLM de plus par message est accepté ; en échec, on retombe sur les
 * {@code topN} premiers candidats (dégradation non bloquante).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmDocumentReranker implements DocumentReranker {

    /** Regex insensible à la casse et tolérante aux espaces : [C0], [c0], [C 0]... */
    private static final Pattern TOKEN = Pattern.compile("(?i)\\[C\\s*(\\d+)]");

    /** Nombre max de candidats envoyés au LLM reranker (réduit de 40 à 15 pour limiter
     *  le coût et la dilution d'attention — R2b). */
    private static final int MAX_CANDIDATES_FOR_RERANK = 15;

    private final ChatProviderResolver chatProviderResolver;

    @Override
    public List<Document> rerank(String query, List<Document> candidates, int topN) {
        if (candidates == null || candidates.isEmpty() || topN <= 0) {
            return candidates == null ? List.of() : candidates;
        }
        if (candidates.size() <= topN) {
            return candidates;
        }
        // Limiter les candidats envoyés au LLM pour réduire le coût (R2b)
        List<Document> toRerank = candidates.subList(0, Math.min(MAX_CANDIDATES_FOR_RERANK, candidates.size()));
        try {
            String prompt = buildPrompt(query, toRerank);
            String answer = chatProviderResolver.current().prompt()
                    .system("Tu es un moteur de reranking. Classe les fragments de cours ci-dessous par "
                            + "pertinence à la question posée. Réponds UNIQUEMENT par la liste des tokens "
                            + "récupérés, du plus pertinent au moins pertinent, un par ligne.")
                    .user(prompt)
                    .call()
                    .content();
            List<Integer> order = parseOrder(answer);
            if (order.size() < topN) {
                log.warn("Rerank LLM ambigu ({} positions retournées pour {} candidats), repli sur topK brut",
                        order.size(), toRerank.size());
                return toRerank.subList(0, Math.min(topN, toRerank.size()));
            }
            Map<Integer, Document> byIndex = new LinkedHashMap<>();
            for (int i = 0; i < toRerank.size(); i++) {
                byIndex.put(i, toRerank.get(i));
            }
            List<Document> ranked = new ArrayList<>();
            for (Integer idx : order) {
                if (ranked.size() >= topN) {
                    break;
                }
                Document doc = byIndex.remove(idx);
                if (doc != null) {
                    ranked.add(doc);
                }
            }
            return ranked;
        } catch (Exception e) {
            log.warn("Rerank LLM échoué ({} candidats), repli sur topK brut : {}", toRerank.size(), e.getMessage());
            return toRerank.subList(0, Math.min(topN, toRerank.size()));
        }
    }

    private String buildPrompt(String query, List<Document> candidates) {
        StringBuilder sb = new StringBuilder("Question : ").append(query).append("\n\nFragments :\n");
        for (int i = 0; i < candidates.size(); i++) {
            sb.append("[C").append(i).append("] ").append(candidates.get(i).getText()).append("\n\n");
        }
        return sb.toString().strip();
    }

    /** Extrait l'ordre [C0],[C2],[C1]… de la réponse LLM (insensible au format). */
    private List<Integer> parseOrder(String answer) {
        List<Integer> order = new ArrayList<>();
        if (answer == null) {
            return order;
        }
        Matcher matcher = TOKEN.matcher(answer);
        while (matcher.find()) {
            try {
                int idx = Integer.parseInt(matcher.group(1));
                if (!order.contains(idx)) {
                    order.add(idx);
                }
            } catch (NumberFormatException ignored) {
                // token illisible ignoré
            }
        }
        return order;
    }
}