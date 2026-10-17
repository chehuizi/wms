package com.wms.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 排序字段白名单校验测试，防止 sortBy 拼接注入 SQL。
 */
class BaseQueryTest {

    @Test
    @DisplayName("合法排序字段映射到数据库列名")
    void shouldMapWhitelistedSortField() {
        WarehouseQuery query = new WarehouseQuery();
        query.setSortBy("code");
        query.setSortOrder("asc");

        assertThat(query.getOrderByClause()).isEqualTo(" ORDER BY code ASC, id DESC");
    }

    @Test
    @DisplayName("非法排序字段回退到 id")
    void shouldFallbackToIdForUnknownSortField() {
        WarehouseQuery query = new WarehouseQuery();
        query.setSortBy("name; DROP TABLE wms_warehouse; --");

        assertThat(query.getOrderByClause()).startsWith(" ORDER BY id ");
        assertThat(query.getOrderByClause()).doesNotContain("DROP");
    }

    @Test
    @DisplayName("非法 sortOrder 回退到 desc")
    void shouldFallbackToDescForInvalidOrder() {
        WarehouseQuery query = new WarehouseQuery();
        query.setSortOrder("asc; DELETE FROM x");

        assertThat(query.getOrderByClause()).isEqualTo(" ORDER BY id DESC, id DESC");
    }

    @Test
    @DisplayName("分页 size 超过 200 时被截断，page 小于 1 时回退到 1")
    void shouldClampPagingParams() {
        WarehouseQuery query = new WarehouseQuery();
        query.setSize(9999);
        query.setPage(0);

        assertThat(query.getResolvedSize()).isEqualTo(200);
        assertThat(query.getResolvedPage()).isEqualTo(1);
        assertThat(query.getOffset()).isEqualTo(0);
    }

    @Test
    @DisplayName("offset 按 (page-1)*size 计算")
    void shouldComputeOffset() {
        WarehouseQuery query = new WarehouseQuery();
        query.setPage(3);
        query.setSize(20);

        assertThat(query.getOffset()).isEqualTo(40);
    }
}
