package io.prometheus.jmx;

import org.yaml.snakeyaml.constructor.Constructor;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilteringSafeConstructor extends Constructor {

    private Set<String> allowedClassNameSet;

    public FilteringSafeConstructor() {
        super();

        allowedClassNameSet = new HashSet<String>();
        allowedClassNameSet.add(null);
        allowedClassNameSet.add(BigInteger.class.getName());
        allowedClassNameSet.add(Boolean.class.getName());
        allowedClassNameSet.add(Byte.class.getName());
        allowedClassNameSet.add(Character.class.getName());
        allowedClassNameSet.add(Date.class.getName());
        allowedClassNameSet.add(java.sql.Date.class.getName());
        allowedClassNameSet.add(Double.class.getName());
        allowedClassNameSet.add(Float.class.getName());
        allowedClassNameSet.add(Integer.class.getName());
        allowedClassNameSet.add(List.class.getName());
        allowedClassNameSet.add(Long.class.getName());
        allowedClassNameSet.add(Map.class.getName());
        allowedClassNameSet.add(Set.class.getName());
        allowedClassNameSet.add(Short.class.getName());
        allowedClassNameSet.add(String.class.getName());
        allowedClassNameSet.add(Timestamp.class.getName());
    }

    @Override
    protected Class<?> getClassForName(String name) throws ClassNotFoundException {
        if (!allowedClassNameSet.contains(name)) {
            throw new IllegalStateException(String.format("Class [%s] isn't allowed", name));
        }

        return super.getClassForName(name);
    }
}
