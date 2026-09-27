package com.acme.salary.dto;

import java.util.List;

/**
 * A page of results in a shape of our own, rather than Spring's {@code Page}, whose JSON is not a
 * stable contract to build a UI against.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
