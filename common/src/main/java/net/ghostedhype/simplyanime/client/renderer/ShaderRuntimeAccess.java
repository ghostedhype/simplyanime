package net.ghostedhype.simplyanime.client.renderer;

import java.lang.reflect.Method;

final class ShaderRuntimeAccess {

    private static final IrisAccess IRIS = findIris();

    private ShaderRuntimeAccess() {
    }

    static boolean packInUse() {
        return IRIS != null
                && Boolean.TRUE.equals(invoke(IRIS.pack(), IRIS.instance()));
    }

    static boolean shadowPass() {
        return IRIS != null
                && Boolean.TRUE.equals(invoke(IRIS.shadow(), IRIS.instance()));
    }

    static String packName() {
        if (IRIS == null || !packInUse()) {
            return "";
        }
        Object value = invoke(IRIS.name(), null);
        return value instanceof String text ? text : "";
    }

    private static IrisAccess findIris() {
        for (String prefix : new String[]{"net.irisshaders.iris", "net.coderbot.iris"}) {
            Class<?> api = findClass(prefix + ".api.v0.IrisApi");
            if (api == null) {
                continue;
            }
            Object instance = invoke(findMethod(api, "getInstance"), null);
            Method pack = findMethod(api, "isShaderPackInUse");
            Method shadow = findMethod(api, "isRenderingShadowPass");
            if (instance == null || pack == null || shadow == null) {
                continue;
            }

            Method name = findMethod(findClass(prefix + ".Iris"), "getCurrentPackName");
            if (name == null) {
                // Some older versions of Iris expose the API and internals under different roots.
                //   So this just makes sure, in case someone uses an older or customised form
                //   of Iris, pack name can still be located. This will allow for back porting
                //   since that is something that you mentioned you were considering.
                String other = prefix.equals("net.irisshaders.iris")
                        ? "net.coderbot.iris" : "net.irisshaders.iris";
                name = findMethod(findClass(other + ".Iris"), "getCurrentPackName");
            }
            return new IrisAccess(instance, pack, shadow, name);
        }
        return null;
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name, false, ShaderRuntimeAccess.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name) {
        if (type == null) {
            return null;
        }
        try {
            return type.getMethod(name);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static Object invoke(Method method, Object instance) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(instance);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private record IrisAccess(Object instance, Method pack, Method shadow, Method name) {
    }
}