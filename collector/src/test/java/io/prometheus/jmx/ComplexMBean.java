package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface ComplexMBean {
    public int getOne();
    public int getTwo();
    public int getThree();
    public int getFour();
    public int getFive();
    public int getsix();
    public int getSeven();
    public int getEight();
    public int getNine();
    public int getTen();
    public int getEleven();
    public int getTwelve();
    public int getThirteen();
    public int getFourteen();
    public int getFifteen();
    public int getSixteen();
}

class Complex implements ComplexMBean {

    public static void registerBean(MBeanServer mbs)
            throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName("Complex:Type=Test");
        Complex mbean = new Complex();
        mbs.registerMBean(mbean, mbeanName);
    }

    public int getOne() {
        return 1;
    }

    public int getTwo() {
        return 2;
    }

    public int getThree() {
        return 3;
    }

    public int getFour() {
        return 4;
    }

    public int getFive() {
        return 5;
    }

    public int getsix() {
        return 6;
    }

    public int getSeven() {
        return 7;
    }

    public int getEight() {
        return 8;
    }

    public int getNine() {
        return 9;
    }

    public int getTen() {
        return 10;
    }

    public int getEleven() {
        return 11;
    }

    public int getTwelve() {
        return 12;
    }

    public int getThirteen() {
        return 13;
    }

    public int getFourteen() {
        return 14;
    }

    public int getFifteen() {
        return 15;
    }

    public int getSixteen() {
        return 16;
    }
}

