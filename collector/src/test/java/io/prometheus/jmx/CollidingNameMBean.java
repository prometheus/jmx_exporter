package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface CollidingNameMBean {

    int getValue();
}

class CollidingName implements CollidingNameMBean {

    private final int value;

    public CollidingName(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return 345;
    }

    static void registerBeans(MBeanServer mbs) {
        // Loop through the entire ASCII character set
        for (int i = 0; i < 127; i++) {
            try {
                // Create and try to register an MBean with the name
                ObjectName objectName =
                        new ObjectName(
                                "io.prometheus.jmx.test:type=Colliding" + ((char) i) + "Name");
                CollidingName collidingName = new CollidingName(i);
                mbs.registerMBean(collidingName, objectName);
            } catch (Throwable t) {
                // Ignore since we are testing all ASCII characters, which may not be allowed in an
                // ObjectName
            }
        }
    }
}
