package com.company.parser.core;

public enum Competitor {
    AGRUPP("AGRUPP"),
    KTZ("КТЗ"),
    NTPZ("НТЗ"),
    METALLSERVICE("METALLSERVICE"),
    METALLOTORG("МеталлоТорг"),
    DIPOS("DIPOS"),
    DEMIDOV("ГК Демидов");

    private final String title;

    Competitor(String title) {
        this.title = title;
    }

    /** Человекочитаемое имя (используй при необходимости в UI/Excel). */
    public String title() { return title; }
}
