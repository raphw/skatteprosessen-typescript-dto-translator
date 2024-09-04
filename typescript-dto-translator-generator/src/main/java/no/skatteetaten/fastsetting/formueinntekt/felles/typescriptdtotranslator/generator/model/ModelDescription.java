package no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.model;

import no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.StructuralResolver;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public interface ModelDescription {

    Class<?> getType();

    void accept(
        BiConsumer<ModelDescription, Map<String, Property>> onBranch,
        Consumer<List<String>> onEnumeration,
        Runnable onLeaf
    );

    <T> T apply(
        Supplier<T> onBranch,
        Supplier<T> onLeaf
    );

    void traverse(
        OnBranch onBranch,
        OnEnumeration onEnumeration,
        Consumer<Class<?>> onLeaf
    ) throws IOException;

    static List<ModelDescription> of(
        Function<Class<?>, StructuralResolver<?>> structuralResolver,
        BiPredicate<Class<?>, String> condition,
        List<Class<?>> types
    ) {
        Set<Class<?>> resolved = new HashSet<>();
        Map<Class<?>, ModelDescription> references = new HashMap<>();
        return types.stream().distinct()
            .map(type -> of(structuralResolver.apply(type), condition, type, resolved, references))
            .collect(Collectors.toList());
    }

    private static <PROPERTY> ModelDescription of(
        StructuralResolver<PROPERTY> structuralResolver,
        BiPredicate<Class<?>, String> condition,
        Class<?> type,
        Set<Class<?>> resolved,
        Map<Class<?>, ModelDescription> references
    ) {
        if (!resolved.add(type)) {
            return new ModelRecursiveDescription(() -> references.get(type));
        }
        ModelDescription description;
        if (type.isEnum()) {
            description = new ModelEnumerationDescription(
                type,
                Arrays.stream(type.getEnumConstants())
                    .map(value -> structuralResolver.getEnumValue((Enum<?>) value))
                    .collect(Collectors.toList())
            );
        } else if (type.isPrimitive() || !structuralResolver.isBranch(type)) {
            description = new ModelLeafDescription(type);
        } else {
            Map<String, Property> properties = new LinkedHashMap<>();
            for (PROPERTY property : structuralResolver.getProperties(type)) {
                String name = structuralResolver.getName(property).replace("[^A-Za-z0-9]", "_");
                if (!condition.test(type, name)) {
                    continue;
                }
                Class<?> target = structuralResolver.getType(property);
                boolean array;
                if (List.class.isAssignableFrom(target)) {
                    Type generic = structuralResolver.getGenericType(property);
                    if (!(generic instanceof ParameterizedType)
                        || ((ParameterizedType) generic).getActualTypeArguments().length != 1
                        || !(((ParameterizedType) generic).getActualTypeArguments()[0] instanceof Class<?>)) {
                        throw new IllegalArgumentException("Unexpected generic type for " + property);
                    }
                    target = (Class<?>) ((ParameterizedType) generic).getActualTypeArguments()[0];
                    array = true;
                } else if (Collection.class.isAssignableFrom(target) || Map.class.isAssignableFrom(target)) {
                    throw new IllegalArgumentException("Only list collection types are supported: " + property);
                } else {
                    array = false;
                }
                properties.put(name, new Property(
                    of(structuralResolver, condition, target, resolved, references),
                    array,
                    !(target.isPrimitive() || structuralResolver.isRequired(property, target))
                ));
            }
            description = new ModelBranchDescription(
                type,
                properties,
                structuralResolver.getSuperClass(type)
                    .map(superType -> of(structuralResolver, condition, superType, resolved, references))
                    .orElse(null),
                structuralResolver.getSubClasses(type).stream()
                    .map(subType -> of(structuralResolver, condition, subType, resolved, references))
                    .collect(Collectors.toList())
            );
        }
        references.put(type, description);
        return description;
    }

    class Property {

        private final ModelDescription description;

        private final boolean array, optional;

        Property(ModelDescription description, boolean array, boolean optional) {
            this.description = description;
            this.array = array;
            this.optional = optional;
        }

        public ModelDescription getDescription() {
            return description;
        }

        public boolean isArray() {
            return array;
        }

        public boolean isOptional() {
            return optional;
        }
    }

    @FunctionalInterface
    interface OnBranch {

        void accept(
            Class<?> type,
            ModelDescription superType,
            List<ModelDescription> subTypes,
            Map<String, Property> properties
        ) throws IOException;
    }

    @FunctionalInterface
    interface OnEnumeration {

        void accept(
            Class<?> type,
            List<String> enumerations
        ) throws IOException;
    }
}
