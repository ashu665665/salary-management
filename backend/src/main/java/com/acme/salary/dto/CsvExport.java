package com.acme.salary.dto;

/** A file to hand to the browser: what to call it, and what is in it. */
public record CsvExport(String filename, String content) {
}
