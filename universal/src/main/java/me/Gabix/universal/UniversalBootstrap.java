package me.Gabix.universal;

import java.lang.reflect.InvocationTargetException;

/**
 * Reflection bridge to the version-specific entrypoint.
 *
 * <p>The universal project is deliberately compiled without Minecraft on its
 * classpath.  This keeps it impossible for an accidentally imported versioned
 * class to end up in the unrelocated bootstrap.</p>
 */
public final class UniversalBootstrap {
    private UniversalBootstrap() {
    }

    public static void invoke(String classSuffix, String method) {
        String className = VersionSelector.prefix() + "." + classSuffix;
        try {
            Class<?> entrypoint = Class.forName(className);
            Object instance = entrypoint.getDeclaredConstructor().newInstance();
            entrypoint.getMethod(method).invoke(instance);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException
                    ? ((InvocationTargetException) exception).getTargetException()
                    : exception;
            throw new IllegalStateException("MVP: failed to invoke " + className + "#" + method, cause);
        }
    }
}
