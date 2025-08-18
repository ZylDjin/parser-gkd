package com.company.parser.core;

public enum Category {
    SP("Профильные трубы");

    private final String title;

    Category(String title) {
        this.title = title;
    }

    public String title() { return title; }

    @Override
    public String toString() { return name(); }
}
