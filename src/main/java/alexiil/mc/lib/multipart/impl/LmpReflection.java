package alexiil.mc.lib.multipart.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Internal - contains some methods for getting LMP fields & methods in the api that are annotated with
 * {@link LmpInternalOnly}.
 * <p>
 * AFAIK this should be safe with modules, assuming the LMP api and implementation stay in the same module. */
public final class LmpReflection {

    public interface ThrowingFunction<I, O, T extends Throwable> {
        O call(I input) throws T;
    }

    public interface ThrowingBiFunction<A, B, O, T extends Throwable> {
        O call(A arg1, B arg2) throws T;
    }

    /** Retrieves a value from a static LMP field that is annotated with {@link LmpInternalOnly}. */
    public static <T> T getStaticApiField(Class<?> from, String field, Class<T> fieldType) {
        try {
            checkPackage(from);
            Field fld = from.getDeclaredField(field);

            if (fld.getAnnotation(LmpInternalOnly.class) == null) {
                throw new Error(
                    "Tried to access a non-internally exposed field! (" + from + " ." + field + " of " + fieldType + ")"
                );
            }

            fld.setAccessible(true);
            checkFieldType(from, field, fieldType, fld);
            if ((fld.getModifiers() & Modifier.STATIC) == 0) {
                throw new Error(
                    "LMP field is not static when we expected it to be static! (" + from + " ." + field + " of "
                        + fieldType + ")"
                );
            }
            return fieldType.cast(fld.get(null));
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new Error(
                "LMP failed to access it's own field?! (" + from + " ." + field + " of " + fieldType + ")", e
            );
        }
    }

    /** Retrieves a function that returns the value held in a non-static LMP field that is annotated with
     * {@link LmpInternalOnly}. */
    public static <C, F> Function<C, F> getInstanceApiField(Class<C> from, String field, Class<F> fieldType) {
        try {
            checkPackage(from);
            Field fld = from.getDeclaredField(field);

            if (fld.getAnnotation(LmpInternalOnly.class) == null) {
                throw new Error(
                    "Tried to access a non-internally exposed field! (" + from + " ." + field + " of " + fieldType + ")"
                );
            }

            fld.setAccessible(true);
            checkFieldType(from, field, fieldType, fld);
            if ((fld.getModifiers() & Modifier.STATIC) != 0) {
                throw new Error(
                    "LMP field is static when we expected it not to be! (" + from + " ." + field + " of " + fieldType
                        + ")"
                );
            }

            return instance -> {
                try {
                    return fieldType.cast(fld.get(instance));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new Error(
                        "LMP failed to access it's own field?! (" + from + " ." + field + " of " + fieldType + ")", e
                    );
                }
            };
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new Error(
                "LMP failed to access it's own field?! (" + from + " ." + field + " of " + fieldType + ")", e
            );
        }
    }

    private static void checkFieldType(Class<?> from, String field, Class<?> expectedType, Field fld) throws Error {

        Class<?> foundType = fld.getType();

        if (foundType.isPrimitive() && !expectedType.isPrimitive()) {
            if (foundType == Character.TYPE) foundType = Character.class;
            else if (foundType == Boolean.TYPE) foundType = Boolean.class;
            else if (foundType == Byte.TYPE) foundType = Byte.class;
            else if (foundType == Short.TYPE) foundType = Short.class;
            else if (foundType == Integer.TYPE) foundType = Integer.class;
            else if (foundType == Long.TYPE) foundType = Long.class;
            else if (foundType == Float.TYPE) foundType = Float.class;
            else if (foundType == Double.TYPE) foundType = Double.class;
        }

        if (foundType != expectedType) {
            throw new Error(
                "LMP field type is different! (" + from + " ." + field + ": expecting " + expectedType + ", but got "
                    + foundType + ")"
            );
        }
    }

    /** Retrieves a function that acts as a constructor for the given set of arguments. */
    public static <C> Function<Object[], C> getApiConstructor(Class<C> from, Class<?>... argTypes) {
        return getThrowingApiConstructor(from, RuntimeException.class, argTypes)::call;
    }

    /** Retrieves a function that acts as a constructor for the given set of arguments, that also rethrows a single
     * exception type. */
    public static <C, T extends Throwable> ThrowingFunction<Object[], C, T> getThrowingApiConstructor(
        Class<C> from, Class<T> exceptionType, Class<?>... argTypes
    ) {
        try {
            checkPackage(from);
            Constructor<C> ctor = from.getDeclaredConstructor(argTypes);

            if (ctor.getAnnotation(LmpInternalOnly.class) == null) {
                throw new Error(
                    "Tried to access a non-internally exposed constructor! (" + from + " of "
                        + Arrays.toString(argTypes) + ")"
                );
            }

            if (exceptionType != null && Throwable.class == exceptionType) {
                throw new Error("Don't catch all throwables -_-");
            }

            for (Class<?> ex : ctor.getExceptionTypes()) {
                if (Error.class.isAssignableFrom(ex)) {
                    continue;
                }
                if (RuntimeException.class.isAssignableFrom(ex)) {
                    continue;
                }

                if (exceptionType == null) {
                    throw new Error(
                        "Tried to access a constructor that throws " + ex + " without declaring a way to catch it!"
                    );
                } else if (!exceptionType.isAssignableFrom(ex)) {
                    throw new Error(
                        "Tried to access a constructor that throws " + ex + " but we only catch " + exceptionType
                    );
                }
            }

            ctor.setAccessible(true);
            return args -> {
                try {
                    return ctor.newInstance(args);
                } catch (InvocationTargetException e) {
                    throw rethrowException(exceptionType, e);
                } catch (IllegalArgumentException e) {
                    throw new Error(
                        "LMP passed the wrong types to it's own constructor?! (" + from + " of "
                            + Arrays.toString(argTypes) + " passed " + Arrays.toString(args) + ")",
                        e
                    );
                } catch (ReflectiveOperationException e) {
                    throw new Error(
                        "LMP failed to access it's own constructor?! (" + from + " of " + Arrays.toString(argTypes)
                            + ")",
                        e
                    );
                }
            };
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new Error(
                "LMP failed to access it's own constructor?! (" + from + " of " + Arrays.toString(argTypes) + ")", e
            );
        }
    }

    /** Retrieves a function that calls a static method with the given arguments. */
    public static <C, R> Function<Object[], R> getStaticApiMethod(
        Class<C> from, String methodName, Class<R> returnType, Class<?>... argTypes
    ) {
        return getStaticThrowingApiMethod(from, methodName, returnType, RuntimeException.class, argTypes)::call;
    }

    /** Retrieves a function that calls a static method with the given arguments, that also rethrows a single exception
     * type. */
    public static <C, R, T extends Throwable> ThrowingFunction<Object[], R, T> getStaticThrowingApiMethod(
        Class<C> from, String methodName, Class<R> returnType, Class<T> exceptionType, Class<?>... argTypes
    ) {
        try {
            checkPackage(from);
            Method method = from.getDeclaredMethod(methodName, argTypes);

            if (method.getAnnotation(LmpInternalOnly.class) == null) {
                throw new Error(
                    "Tried to access a non-internally exposed method! (" + from + " of " + Arrays.toString(argTypes)
                        + ")"
                );
            }

            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                throw new Error("LMP method is not static when we expected it to be! (" + from + " ." + method + ")");
            }

            if (exceptionType != null && Throwable.class == exceptionType) {
                throw new Error("Don't catch all throwables -_-");
            }

            for (Class<?> ex : method.getExceptionTypes()) {
                if (Error.class.isAssignableFrom(ex)) {
                    continue;
                }
                if (RuntimeException.class.isAssignableFrom(ex)) {
                    continue;
                }

                if (exceptionType == null) {
                    throw new Error(
                        "Tried to access a method that throws " + ex + " without declaring a way to catch it!"
                    );
                } else if (!exceptionType.isAssignableFrom(ex)) {
                    throw new Error(
                        "Tried to access a method that throws " + ex + " but we only catch " + exceptionType
                    );
                }
            }

            method.setAccessible(true);
            return args -> {
                try {
                    Object ret = method.invoke(null, args);
                    if (ret == null) {
                        return null;
                    } else if (returnType.isInstance(ret)) {
                        return returnType.cast(ret);
                    } else {
                        throw new Error("The method returned value was not of the right class!");
                    }
                } catch (InvocationTargetException e) {
                    throw rethrowException(exceptionType, e);
                } catch (IllegalArgumentException e) {
                    throw new Error(
                        "LMP passed the wrong types to it's own method?! (" + from + " of " + Arrays.toString(argTypes)
                            + " passed " + Arrays.toString(args) + ")",
                        e
                    );
                } catch (ReflectiveOperationException e) {
                    throw new Error(
                        "LMP failed to access it's own method?! (" + from + " of " + Arrays.toString(argTypes) + ")", e
                    );
                }
            };
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new Error(
                "LMP failed to access it's own method?! (" + from + " of " + Arrays.toString(argTypes) + ")", e
            );
        }
    }

    /** Retrieves a function that calls an instance method with the given arguments. */
    public static <C, R> BiFunction<C, Object[], R> getInstanceApiMethod(
        Class<C> from, String methodName, Class<R> returnType, Class<?>... argTypes
    ) {
        return getInstanceThrowingApiMethod(from, methodName, returnType, RuntimeException.class, argTypes)::call;
    }

    /** Retrieves a function that calls an instance method with the given arguments, that also rethrows a single
     * exception type. */
    public static <C, R, T extends Throwable> ThrowingBiFunction<C, Object[], R, T> getInstanceThrowingApiMethod(
        Class<C> from, String methodName, Class<R> returnType, Class<T> exceptionType, Class<?>... argTypes
    ) {
        try {
            checkPackage(from);
            Method method = from.getDeclaredMethod(methodName, argTypes);

            if (method.getAnnotation(LmpInternalOnly.class) == null) {
                throw new Error(
                    "Tried to access a non-internally exposed method! (" + from + " of " + Arrays.toString(argTypes)
                        + ")"
                );
            }

            if ((method.getModifiers() & Modifier.STATIC) != 0) {
                throw new Error("LMP method is static when we expected it not to be! (" + from + " ." + method + ")");
            }

            if (exceptionType != null && Throwable.class == exceptionType) {
                throw new Error("Don't catch all throwables -_-");
            }

            for (Class<?> ex : method.getExceptionTypes()) {
                if (Error.class.isAssignableFrom(ex)) {
                    continue;
                }
                if (RuntimeException.class.isAssignableFrom(ex)) {
                    continue;
                }

                if (exceptionType == null) {
                    throw new Error(
                        "Tried to access a method that throws " + ex + " without declaring a way to catch it!"
                    );
                } else if (!exceptionType.isAssignableFrom(ex)) {
                    throw new Error(
                        "Tried to access a method that throws " + ex + " but we only catch " + exceptionType
                    );
                }
            }

            method.setAccessible(true);
            return (instance, args) -> {
                try {
                    Object ret = method.invoke(instance, args);
                    if (ret == null) {
                        return null;
                    } else if (returnType.isInstance(ret)) {
                        return returnType.cast(ret);
                    } else {
                        throw new Error("The method returned value was not of the right class!");
                    }
                } catch (InvocationTargetException e) {
                    throw rethrowException(exceptionType, e);
                } catch (IllegalArgumentException e) {
                    throw new Error(
                        "LMP passed the wrong types to it's own method?! (" + from + " of " + Arrays.toString(argTypes)
                            + " passed " + Arrays.toString(args) + ")",
                        e
                    );
                } catch (ReflectiveOperationException e) {
                    throw new Error(
                        "LMP failed to access it's own method?! (" + from + " of " + Arrays.toString(argTypes) + ")", e
                    );
                }
            };
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new Error(
                "LMP failed to access it's own method?! (" + from + " of " + Arrays.toString(argTypes) + ")", e
            );
        }
    }

    private static <T extends Throwable> T rethrowException(Class<T> exceptionType, InvocationTargetException e)
        throws T {

        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException re) {
            throw re;
        }
        if (cause instanceof Error er) {
            throw er;
        }

        if (exceptionType == null) {
            throw new Error("No exception type was declared, but a checked exception was thrown!", cause);
        } else if (exceptionType.isInstance(cause)) {
            throw exceptionType.cast(cause);
        } else {
            throw new Error(
                "An exception type was declared, but a checked exception of a different type was thrown!", cause
            );
        }
    }

    private static void checkPackage(Class<?> from) {
        if (from.isArray() || from.isPrimitive()) {
            throw new Error("These methods only work with LMP classes - not arrays or primitives");
        }
        if (!from.getPackageName().startsWith("alexiil.mc.lib.multipart.api")) {
            throw new Error("Tried to retieve something from outside of LMP's API! (" + from + ")");
        }
    }
}
