package io.prometheus.jmx;

public enum State {
    RUNNING(1, 2),
    TERMINATED(2, 3);

    private int valueOne;

    private int valueTwo;

    State(int valueOne, int valueTwo) {
        this.valueOne = valueOne;
        this.valueTwo = valueTwo;
    }

    public int getValueOne() {
        return valueOne;
    }

    public int getValueTwo() {
        return valueTwo;
    }
}
