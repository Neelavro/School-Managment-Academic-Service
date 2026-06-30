package com.example.academic_service.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable JSON shape for paginated endpoints.
 *
 * Spring's default {@code Page<T>} serialization is unstable across
 * versions (Boot 3.3+ wraps under a {@code page} key, older versions
 * keep totalElements/totalPages at the top level, Boot 4.x may change
 * again). Controllers that previously returned {@code Page<T>} now
 * return this DTO so the frontend always sees the same keys:
 *
 *   { content: [...], number: 0, size: 30, totalElements: 50, totalPages: 2, first: true, last: false }
 */
@Getter
@Setter
public class PagedResponse<T> {
    private List<T> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    public static <S, T> PagedResponse<T> of(Page<S> page, Function<S, T> mapper) {
        PagedResponse<T> out = new PagedResponse<>();
        out.content = page.getContent().stream().map(mapper).toList();
        out.number = page.getNumber();
        out.size = page.getSize();
        out.totalElements = page.getTotalElements();
        out.totalPages = page.getTotalPages();
        out.first = page.isFirst();
        out.last = page.isLast();
        out.empty = page.isEmpty();
        return out;
    }

    public static <T> PagedResponse<T> of(Page<T> page) {
        return of(page, t -> t);
    }
}
