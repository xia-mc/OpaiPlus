package asia.lira.opaiplus.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class ReflectionUtils {
    /**
     * 调用方法
     */
    @SuppressWarnings("unchecked")
    public static <T> T call(@NotNull Object object, @NotNull String method, Object... params) {
        final MethodData data = new MethodData(object.getClass(), method, params);

        try {
            return (T) getMethod(data).invoke(object, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用方法，并指定类型
     */
    @SuppressWarnings("unchecked")
    public static <T> T call(@NotNull Object object, @NotNull String method,
                             Class<?> @NotNull [] paramTypes, Object @NotNull [] params) {
        if (params.length != paramTypes.length) {
            throw new RuntimeException("Param length not match with paramTypes length");
        }

        final MethodData data = new MethodData(object.getClass(), method, paramTypes);

        try {
            return (T) getMethod(data).invoke(object, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    @AllArgsConstructor
    private static final class MethodData {
        private final Class<?> aClass;
        private final String method;
        private final Class<?>[] params;

        MethodData(@NotNull Class<?> aClass, @NotNull String method, Object... params) {
            this(aClass, method, Arrays.stream(params).map(Object::getClass).toArray(Class[]::new));
        }
    }

    @Data
    @AllArgsConstructor
    private static final class FieldData {
        private final Class<?> aClass;
        private final String field;
    }

    @Data
    @AllArgsConstructor
    private static final class ConstructorData {
        private final Class<?> aClass;
        private final Class<?>[] params;

        ConstructorData(@NotNull Class<?> aClass, Object... params) {
            this(aClass, Arrays.stream(params).map(Object::getClass).toArray(Class[]::new));
        }
    }

    private static final Map<String, Class<?>> classMap = new Object2ObjectOpenHashMap<>();
    private static final Map<MethodData, Method> methodMap = new Object2ObjectOpenHashMap<>();
    private static final Map<ConstructorData, Constructor<?>> constuctorMap = new Object2ObjectOpenHashMap<>();
    private static final Map<FieldData, Field> fieldMap = new Object2ObjectOpenHashMap<>();
    private static final Map<String, Consumer<?>> fastMethodAnArgMap = new Object2ObjectOpenHashMap<>();
    private static final Map<String, Runnable> fastMethodNoArgMap = new Object2ObjectOpenHashMap<>();
    private static final Map<String, Supplier<?>> fastMethodRetMap = new Object2ObjectOpenHashMap<>();
    private static final Map<String, BooleanSupplier> fastMethodRetBooleanMap = new Object2ObjectOpenHashMap<>();

    /**
     * 获取方法，递归查找父类中的方法
     */
    private static @NotNull Method getMethod(@NotNull MethodData data) {
        Method result = methodMap.get(data);
        if (result != null)
            return result;

        Class<?> currentClass = data.getAClass();
        while (currentClass != null) {
            try {
                final Method target = currentClass.getDeclaredMethod(data.getMethod(), data.getParams());
                target.setAccessible(true);
                methodMap.put(data, target);
                return target;
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();  // 继续查找父类
            }
        }
        throw new RuntimeException("Method not found: " + data.getMethod());
    }

    /**
     * 获取构造器
     */
    private static @NotNull Constructor<?> getConstructor(@NotNull ConstructorData data) {
        Constructor<?> result = constuctorMap.get(data);
        if (result != null)
            return result;

        try {
            result = data.getAClass().getDeclaredConstructor(data.getParams());
            result.setAccessible(true);
            constuctorMap.put(data, result);
            return result;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Constructor not found: " + data.getAClass().getName());
        }
    }

    /**
     * 获取字段，递归查找父类中的字段
     */
    private static @NotNull Field getField(@NotNull FieldData data) {
        Field result = fieldMap.get(data);
        if (result != null)
            return result;

        Class<?> currentClass = data.getAClass();
        while (currentClass != null) {
            try {
                final Field target = currentClass.getDeclaredField(data.getField());
                target.setAccessible(true);

                int modifiers = target.getModifiers();
                if (Modifier.isFinal(modifiers)) {
                    ReflectionUtils.set(target, "modifiers", modifiers & ~Modifier.FINAL);
                }

                fieldMap.put(data, target);
                return target;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();  // 继续查找父类
            }
        }
        throw new RuntimeException("Field not found: " + data.getField());
    }

    /**
     * 获取类
     */
    @SuppressWarnings("unchecked")
    public static @NotNull <T extends Class<?>> T getClass(@NotNull String className) {
        Class<?> result = classMap.get(className);
        if (result != null) {
            return (T) result;
        }

        try {
            result = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className);
        }
        classMap.put(className, result);
        return (T) result;
    }

    /**
     * 调用静态方法
     */
    @SuppressWarnings("unchecked")
    public static <T> T callDeclared(@NotNull Class<?> aClass, @NotNull String method, Object... params) {
        final MethodData data = new MethodData(aClass, method, params);

        try {
            return (T) getMethod(data).invoke(null, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用静态方法
     */
    @SuppressWarnings("unchecked")
    public static <T> T callDeclared(@NotNull Class<?> aClass, @NotNull String method, Class<?>[] paramTypes, Object[] params) {
        final MethodData data = new MethodData(aClass, method, paramTypes);

        try {
            return (T) getMethod(data).invoke(null, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用构造器
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull T callConstructor(@NotNull Class<T> aClass, Object... params) {
        final ConstructorData data = new ConstructorData(aClass, params);

        try {
            return (T) getConstructor(data).newInstance(params);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用构造器
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull T callConstructor(@NotNull Class<T> aClass, Class<?>[] paramTypes, Object[] params) {
        final ConstructorData data = new ConstructorData(aClass, paramTypes);

        try {
            return (T) getConstructor(data).newInstance(params);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取字段值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(@NotNull Object object, @NotNull String field) {
        final FieldData data = new FieldData(object.getClass(), field);

        try {
            return (T) getField(data).get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取静态字段值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getDeclared(@NotNull Class<?> aClass, @NotNull String field) {
        final FieldData data = new FieldData(aClass, field);

        try {
            return (T) getField(data).get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置字段值
     */
    public static void set(@NotNull Object object, @NotNull String field, Object value) {
        final FieldData data = new FieldData(object.getClass(), field);

        try {
            getField(data).set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置静态字段值
     */
    public static void setDeclared(@NotNull Class<?> aClass, @NotNull String field, Object value) {
        final FieldData data = new FieldData(aClass, field);

        try {
            getField(data).set(null, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, E> Consumer<T> getFastMethodAnArg(Class<E> objClass, E object, String methodName, Class<T> argClass)
            throws Throwable {
        Consumer<?> result = fastMethodAnArgMap.get(methodName);
        if (result != null)
            return (Consumer<T>) result;

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method method = objClass.getDeclaredMethod(methodName, argClass);
        MethodHandle methodHandle = lookup.unreflect(method);

        CallSite callSite = LambdaMetafactory.metafactory(
                lookup,
                "accept",
                MethodType.methodType(Consumer.class, objClass),
                MethodType.methodType(void.class, Object.class),
                methodHandle,
                MethodType.methodType(void.class, argClass)
        );

        Consumer<T> target = (Consumer<T>) callSite.getTarget().invoke(object);
        fastMethodAnArgMap.put(methodName, target);
        return target;
    }

    public static <T> Runnable getFastMethodNoArg(Class<T> objClass, T object, String methodName)
            throws Throwable {
        Runnable result = fastMethodNoArgMap.get(methodName);
        if (result != null)
            return result;

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method method = objClass.getDeclaredMethod(methodName);
        MethodHandle methodHandle = lookup.unreflect(method);

        CallSite callSite = LambdaMetafactory.metafactory(
                lookup,
                "run",
                MethodType.methodType(Runnable.class, objClass),
                MethodType.methodType(void.class),
                methodHandle,
                MethodType.methodType(void.class)
        );

        Runnable target = (Runnable) callSite.getTarget().invoke(object);
        fastMethodNoArgMap.put(methodName, target);
        return target;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Supplier<R> getFastMethodRet(Class<T> objClass, T object, String methodName, Class<R> retClass)
            throws Throwable {
        Supplier<?> result = fastMethodRetMap.get(methodName);
        if (result != null)
            return (Supplier<R>) result;

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method method = objClass.getDeclaredMethod(methodName);
        MethodHandle methodHandle = lookup.unreflect(method);

        CallSite callSite = LambdaMetafactory.metafactory(
                lookup,
                "get",
                MethodType.methodType(Supplier.class, objClass),
                MethodType.methodType(Object.class),
                methodHandle,
                MethodType.methodType(retClass)
        );

        Supplier<R> target = (Supplier<R>) callSite.getTarget().invoke(object);
        fastMethodRetMap.put(methodName, target);
        return target;
    }

    public static <T> BooleanSupplier getFastMethodRetBoolean(@NotNull Class<T> objClass, T object, String methodName)
            throws Throwable {
        BooleanSupplier result = fastMethodRetBooleanMap.get(methodName);
        if (result != null)
            return result;

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method method = objClass.getDeclaredMethod(methodName);
        MethodHandle methodHandle = lookup.unreflect(method);

        CallSite callSite = LambdaMetafactory.metafactory(
                lookup,
                "getAsBoolean",
                MethodType.methodType(BooleanSupplier.class, objClass),
                MethodType.methodType(boolean.class),
                methodHandle,
                MethodType.methodType(boolean.class)
        );

        BooleanSupplier target = (BooleanSupplier) callSite.getTarget().invoke(object);
        fastMethodRetBooleanMap.put(methodName, target);
        return target;
    }

    public static boolean isFastMethod(Consumer<?> method) {
        return fastMethodAnArgMap.containsValue(method);
    }

    public static boolean isFastMethod(Runnable method) {
        return fastMethodNoArgMap.containsValue(method);
    }

    public static boolean isFastMethod(Supplier<?> method) {
        return fastMethodRetMap.containsValue(method);
    }

    public static @NotNull String getCallerClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 0: getStackTrace
        // 1: this method
        // 2: caller
        // 3: the method called caller
        if (stackTrace.length < 3)
            return "Unknown Source";
        String[] splits = stackTrace[3].getClassName().split("\\.");
        return splits[splits.length - 1];
    }

    public static @NotNull String getCallerClassName(final int extraDepth) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 0: getStackTrace
        // 1: this method
        // 2: caller
        // 3: the method called caller
        if (stackTrace.length < 3 + extraDepth)
            return "Unknown Source";
        String[] splits = stackTrace[3 + extraDepth].getClassName().split("\\.");
        return splits[splits.length - 1];
    }

    public static @NotNull String getCallerMethodName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 0: getStackTrace
        // 1: this method
        // 2: caller
        // 3: the method called caller
        if (stackTrace.length < 3)
            return "Unknown Source";
        return stackTrace[3].getMethodName();
    }

    public static @NotNull String getCallerMethodName(final int extraDepth) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 0: getStackTrace
        // 1: this method
        // 2: caller
        // 3: the method called caller
        if (stackTrace.length < 3 + extraDepth)
            return "Unknown Source";
        return stackTrace[3 + extraDepth].getMethodName();
    }
}
