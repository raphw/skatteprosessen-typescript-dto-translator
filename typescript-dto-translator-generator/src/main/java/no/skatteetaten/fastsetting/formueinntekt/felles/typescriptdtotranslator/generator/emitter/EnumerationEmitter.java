package no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.emitter;

import no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.TypeName;
import no.skatteetaten.fastsetting.formueinntekt.felles.typescriptdtotranslator.generator.model.ModelDescription;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class EnumerationEmitter implements ModelDescription.OnEnumeration {

    private final Appendable appendable;
    private final Function<Class<?>, TypeName> naming;
    private final boolean export;

    public EnumerationEmitter(Appendable appendable, Function<Class<?>, TypeName> naming, boolean export) {
        this.appendable = appendable;
        this.naming = naming;
        this.export = export;
    }

    @Override
    public void accept(
        Class<?> type,
        List<String> enumerations
    ) throws IOException {
        if (export) {
            appendable.append("export ");
        }
        TypeName name = naming.apply(type);
        String indent;
        if (name.getModule().isPresent()) {
            indent = "  ";
            appendable.append("module ")
                .append(name.getModule().orElseThrow())
                .append(" {\n");
        } else {
            indent = "";
        }
        appendable.append(indent);
        if (name.getModule().isPresent()) {
            appendable.append("export ");
        }
        appendable.append("enum ").append(naming.apply(type).getName()).append(" {\n");
        Iterator<String> it = enumerations.iterator();
        while (it.hasNext()) {
            String enumeration = it.next();
            appendable.append(indent).append("  ").append(enumeration).append(" = \"").append(enumeration).append("\"");
            if (it.hasNext()) {
                appendable.append(",");
            }
            appendable.append("\n");
        }
        appendable.append(indent).append("}\n");
        if (name.getModule().isPresent()) {
            appendable.append("}\n");
        }
    }
}
