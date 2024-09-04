package no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class StructuralTypeStructuralResolver implements StructuralResolver<Method> {

    private final Function<Class<?>, String> prefix;

    private final Class<? extends Annotation> compoundOf, subtypedBy;

    private final MethodHandle value;

    @SuppressWarnings("unchecked")
    private StructuralTypeStructuralResolver(Function<Class<?>, String> prefixes, ClassLoader classLoader) {
        this.prefix = prefixes;
        try {
            compoundOf = (Class<? extends Annotation>) Class.forName("no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.CompoundOf", true, classLoader);
            subtypedBy = (Class<? extends Annotation>) Class.forName("no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.SubtypedBy", true, classLoader);
            value = MethodHandles.publicLookup().findVirtual(subtypedBy, "value", MethodType.methodType(Class[].class));
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve structural type API of " + classLoader, e);
        }
    }

    public static StructuralTypeStructuralResolver of() {
        return of(StructuralTypeStructuralResolver.class.getClassLoader());
    }

    public static StructuralTypeStructuralResolver of(ClassLoader classLoader) {
        return new StructuralTypeStructuralResolver(type -> type == boolean.class ? "is" : "get", classLoader);
    }

    public static StructuralTypeStructuralResolver of(Function<Class<?>, String> prefix) {
        return new StructuralTypeStructuralResolver(prefix, StructuralTypeStructuralResolver.class.getClassLoader());
    }

    public static StructuralTypeStructuralResolver of(Function<Class<?>, String> prefix, ClassLoader classLoader) {
        return new StructuralTypeStructuralResolver(prefix, classLoader);
    }

    @Override
    public Iterable<Method> getProperties(Class<?> type) {
        return () -> Stream.of(type.getDeclaredMethods())
            .filter(method -> method.getName().startsWith(prefix.apply(method.getReturnType())))
            .filter(method -> !method.isSynthetic())
            .iterator();
    }

    @Override
    public boolean isRequired(Method property, Class<?> type) {
        return false;
    }

    @Override
    public boolean isBranch(Class<?> type) {
        return type.isAnnotationPresent(compoundOf);
    }

    @Override
    public String getName(Method property) {
        String name = property.getName(), prefix = this.prefix.apply(property.getReturnType());
        if (prefix.equals(name)) {
            return "$value";
        } else {
            return name.substring(prefix.length(), prefix.length() + 1).toLowerCase() + name.substring(prefix.length() + 1);
        }
    }

    @Override
    public Class<?> getType(Method property) {
        Class<?> type = property.getReturnType();
        if (type == Optional.class) {
            Type generic = property.getGenericReturnType();
            if (generic instanceof ParameterizedType) {
                Type argument = ((ParameterizedType) generic).getActualTypeArguments()[0];
                if (argument instanceof Class<?>) {
                    return (Class<?>) argument;
                } else {
                    throw new IllegalStateException("Expected optional type to be parameterized for " + property);
                }
            } else {
                throw new IllegalStateException("Expected optional type to be parameterized for " + property);
            }
        } else {
            return type;
        }
    }

    @Override
    public Type getGenericType(Method property) {
        return property.getGenericReturnType();
    }

    @Override
    public Optional<Class<?>> getSuperClass(Class<?> type) {
        return Stream.of(type.getInterfaces())
            .filter(iface -> iface.isAnnotationPresent(compoundOf))
            .findAny();
    }

    @Override
    public List<Class<?>> getSubClasses(Class<?> type) {
        try {
            Annotation annotation = type.getAnnotation(subtypedBy);
            return annotation == null ? List.of() : List.of((Class<?>[]) value.invoke(annotation));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }
}
