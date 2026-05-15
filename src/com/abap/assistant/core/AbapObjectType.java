package com.abap.assistant.core;

public enum AbapObjectType {
    INCLUDE("Include"),
    PROGRAM("Program"),
    FORM("Form routine"),
    FUNCTION_MODULE("Function module"),
    TRANSACTION("Transaction"),
    CLASS("Class"),
    METHOD("Method"),
    TABLE("Table"),
    UNKNOWN("Unknown");

    private final String displayName;

    AbapObjectType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
