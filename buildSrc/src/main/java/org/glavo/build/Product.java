package org.glavo.build;

public enum Product {
    INTELLIJ_COMMUNITY_EDITION("IC"),
    INTELLIJ_ULTIMATE("IU");

    private final String code;

    Product(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
