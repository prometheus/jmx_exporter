package io.prometheus.jmx.common.configuration;

import io.prometheus.jmx.common.util.Precondition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class to convert an Object to a Map, throwing a RuntimeException from the Supplier if there is a
 * ClassCastException
 */
@SuppressWarnings("unchecked")
public class ConvertToMap implements Function<Object, Map<String, String>> {

    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructor
     *
     * @param supplier supplier
     */
    public ConvertToMap(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public Map<String, String> apply(Object o) {
        try {
            Map<String, String> result = new LinkedHashMap<>();
            Map<Object, Object> map = (Map<Object, Object>) o;

            map.forEach((o1, o2) -> result.put(o1.toString(), o2.toString()));

            return result;
        } catch (Throwable t) {
            throw supplier.get();
        }
    }
}
