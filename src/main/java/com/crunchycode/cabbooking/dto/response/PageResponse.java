package com.crunchycode.cabbooking.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Simple JSON shape for every paginated list endpoint.
 * (We return this instead of Spring's Page object so the JSON stays stable and clean.)
 */
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    public PageResponse(List<T> content, int page, int size, long totalElements,
                        int totalPages, boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(springPage.getContent(), springPage.getNumber(), springPage.getSize(),
                springPage.getTotalElements(), springPage.getTotalPages(), springPage.isFirst(),
                springPage.isLast());
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}
