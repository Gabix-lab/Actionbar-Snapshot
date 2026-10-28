package me.Gabix.renderscale;

import java.lang.reflect.Method;

public class IrisCompatibility {

    public static boolean isShaderPackInUse() {
        if (RenderScale.PLATFORM == null || !RenderScale.PLATFORM.isModLoaded("iris")) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return (Boolean) apiClass.getMethod("isShaderPackInUse").invoke(api);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void reloadShaders() {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Method reload = iris.getMethod("reload");
            reload.invoke(null);
        } catch (Throwable ignored) {}
    }
}