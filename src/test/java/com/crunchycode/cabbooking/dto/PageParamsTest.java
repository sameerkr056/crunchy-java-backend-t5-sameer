package com.crunchycode.cabbooking.dto;

import com.crunchycode.cabbooking.dto.request.PageParams;
import com.crunchycode.cabbooking.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit test: no Spring context and no MySQL needed, so "mvn package" works without a database.
 */
class PageParamsTest {

    private static final Set<String> ALLOWED = Set.of("id", "name");

    @Test
    void defaultsAreFirstPageOfTenSortedByIdAscending() {
        Pageable pageable = new PageParams().toPageable(ALLOWED);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void customValuesAreUsed() {
        PageParams params = new PageParams();
        params.setPage(2);
        params.setSize(5);
        params.setSortBy("name");
        params.setDirection("DESC");

        Pageable pageable = params.toPageable(ALLOWED);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void unknownSortFieldIsRejected() {
        PageParams params = new PageParams();
        params.setSortBy("password");

        assertThatThrownBy(() -> params.toPageable(ALLOWED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sortBy");
    }
}
