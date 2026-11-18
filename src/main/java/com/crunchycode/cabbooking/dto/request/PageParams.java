package com.crunchycode.cabbooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.crunchycode.cabbooking.exception.BadRequestException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/**
 * Pagination query parameters shared by every list endpoint:
 * ?page=0&size=10&sortBy=id&direction=asc
 */
public class PageParams {

    @Min(value = 0, message = "page must be 0 or greater")
    @Schema(description = "Page number, starting at 0", example = "0", defaultValue = "0")
    private int page = 0;

    @Min(value = 1, message = "size must be at least 1")
    @Max(value = 100, message = "size must not be greater than 100")
    @Schema(description = "Items per page (1 to 100)", example = "10", defaultValue = "10")
    private int size = 10;

    @Schema(description = "Field to sort by (allowed fields depend on the endpoint, see its description)", example = "id", defaultValue = "id")
    private String sortBy = "id";

    @Pattern(regexp = "(?i)asc|desc", message = "direction must be asc or desc")
    @Schema(description = "Sort direction", allowableValues = {"asc", "desc"}, defaultValue = "asc")
    private String direction = "asc";

    /**
     * Builds a Spring Data Pageable. Only the allowed fields can be used for sorting,
     * so clients cannot sort by random (or unknown) properties.
     */
    public Pageable toPageable(Set<String> allowedSortFields) {
        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException("Invalid sortBy '" + sortBy + "'. Allowed values: "
                    + String.join(", ", new TreeSet<>(allowedSortFields)));
        }
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        if (!"id".equals(sortBy)) {
            // second sort key keeps the order stable between pages
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
