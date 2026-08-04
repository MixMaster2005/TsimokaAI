package mg.esmia.miage.common.context;

/**
 * Porte le UserContext courant sur le thread de la requête (ThreadLocal).
 * Rempli par UserContextFilter, lu par les services/controllers via un @Bean request-scoped
 * (voir UserContextConfig) ou directement via UserContextHolder.get() dans le code non géré par Spring.
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CURRENT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        CURRENT.set(context);
    }

    public static UserContext get() {
        UserContext ctx = CURRENT.get();
        if (ctx == null) {
            // Contexte système (appel interne, job planifié, listener d'événement...)
            return new UserContext(null, "SYSTEM", null);
        }
        return ctx;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
