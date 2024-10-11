package dev.akarah.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


public class Records {

    // Thanks to https://sormuras.github.io/blog/2020-05-06-records-to-text-block.html
    /** Returns a multi-line string representation of the given object. */
    public static String toTextBlock(Object object) {
        return toTextBlock(0, object, "\t");
    }

    // Thanks to https://sormuras.github.io/blog/2020-05-06-records-to-text-block.html
    private static String toTextBlock(int level, Object object, String indent) {

        var lines = new ArrayList<String>();
        if (level == 0) lines.add(object.getClass().getSimpleName());

        var fields = object.getClass().getDeclaredFields();
        Arrays.sort(fields, Comparator.comparing(Field::getName));

        for (var field : fields) {
            try {
                var name = field.getName();
                var method = object.getClass().getDeclaredMethod(name);
                try {
                    var shift = indent.repeat(level);
                    var value = method.invoke(object);
                    var nested = value.getClass();
                    if (nested.isRecord()) {
                        lines.add(String.format("%s%s%s -> %s", shift, indent, name, nested.getSimpleName()));
                        lines.add(toTextBlock(level + 2, value, indent));
                    } else if (Arrays.stream(nested.getInterfaces()).toList().contains(List.class)) {
                        lines.add(shift + indent + name + " = ");
                        for(var subrecord : ((List<Record>) value)) {
                            lines.add(toTextBlock(level + 2, subrecord, indent));
                        }
                    } else {
                        lines.add(String.format("%s%s%s = %s", shift, indent, name, value));
                    }
                } catch (ReflectiveOperationException e) {
                    lines.add("// Reflection over " + method + " failed: " + e);
                }
            } catch (NoSuchMethodException e) {
                lines.add("// Reflection over field " + field + " failed: " + e);
            }
        }
        return String.join(System.lineSeparator(), lines);
    }
}

