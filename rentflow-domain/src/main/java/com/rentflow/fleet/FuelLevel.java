package com.rentflow.fleet;

public enum FuelLevel {
    EMPTY(0),
    QUARTER(25),
    HALF(50),
    THREE_QUARTERS(75),
    FULL(100);

    private final int percentageFull;

    FuelLevel(int percentageFull) {
        this.percentageFull = percentageFull;
    }

    public int percentageFull() {
        return percentageFull;
    }
}
