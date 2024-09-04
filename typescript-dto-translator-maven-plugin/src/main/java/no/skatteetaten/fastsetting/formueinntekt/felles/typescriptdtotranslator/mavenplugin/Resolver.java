package no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.mavenplugin;

import no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.JaxbStructuralResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.SimpleStructuralResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.StructuralResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.StructuralTypeStructuralResolver;

import java.lang.annotation.Annotation;
import java.util.function.Function;

public enum Resolver {
    SIMPLE {
        @Override
        Function<Class<?>, StructuralResolver<?>> resolver(ClassLoader classLoader) {
            return new SimpleStructuralResolver().asFactory();
        }
    },
    STRUCTURAL {
        @Override
        Function<Class<?>, StructuralResolver<?>> resolver(ClassLoader classLoader) {
            return StructuralTypeStructuralResolver.of(classLoader).asFactory();
        }
    },
    JAVAX {
        @Override
        Function<Class<?>, StructuralResolver<?>> resolver(ClassLoader classLoader) {
            return JaxbStructuralResolver.ofJavax(classLoader).asFactory();
        }
    },
    JAKARTA {
        @Override
        Function<Class<?>, StructuralResolver<?>> resolver(ClassLoader classLoader) {
            return JaxbStructuralResolver.ofJakarta(classLoader).asFactory();
        }
    },

    INDUCE {
        @Override
        Function<Class<?>, StructuralResolver<?>> resolver(ClassLoader classLoader) {
            StructuralResolver<?> resolver = new SimpleStructuralResolver();
            Class<? extends Annotation> structural = resolve("no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.CompoundOf", classLoader);
            StructuralResolver<?> resolverStructural = structural == null ? null : StructuralTypeStructuralResolver.of(classLoader);
            Class<? extends Annotation> javax = resolve("javax.xml.bind.annotation.XmlType", classLoader);
            StructuralResolver<?> resolverJavax = javax == null ? null : JaxbStructuralResolver.ofJavax(classLoader);
            Class<? extends Annotation> jakarta = resolve("jakarta.xml.bind.annotation.XmlType", classLoader);
            StructuralResolver<?> resolverJakarta = jakarta == null ? null : JaxbStructuralResolver.ofJakarta(classLoader);
            return type -> {
                if (resolverStructural != null && type.isAnnotationPresent(structural)) {
                    return resolverStructural;
                } else if (resolverJavax != null && type.isAnnotationPresent(javax)) {
                    return resolverJavax;
                } else if (resolverJakarta != null && type.isAnnotationPresent(jakarta)) {
                    return resolverJakarta;
                } else {
                    return resolver;
                }
            };
        }
    };

    @SuppressWarnings("unchecked")
    static Class<? extends Annotation> resolve(String type, ClassLoader classLoader) {
        try {
            return (Class<? extends Annotation>) Class.forName(type, false, classLoader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    abstract Function<Class<?>, StructuralResolver<?>> resolver(ClassLoader classLoader);
}
