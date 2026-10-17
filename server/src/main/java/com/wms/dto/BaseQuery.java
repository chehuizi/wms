package com.wms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Map;

/**
 * 分页查询公共参数，字段与契约第 2 节一致。
 * 子类通过 {@link #sortWhitelist()} 提供排序字段白名单（JSON 字段名 -> 数据库列名）。
 */
@Data
public class BaseQuery {

    /** 默认排序字段 */
    private static final String DEFAULT_SORT_BY = "id";
    private static final String DEFAULT_SORT_ORDER = "desc";

    private Integer page = 1;
    private Integer size = 20;

    /** 模糊搜索，匹配编码 + 名称 */
    private String keyword;

    /** 1=启用 0=停用，不传=全部 */
    private Integer status;

    /** 排序字段（白名单校验，非法回退到 id） */
    private String sortBy;

    /** asc / desc，非法回退到 desc */
    private String sortOrder;

    /** 各模块允许的排序字段：key 为请求里的 JSON 字段名，value 为数据库列名 */
    protected Map<String, String> sortWhitelist() {
        return Map.of();
    }

    // 以下三个方法必须是 JavaBean getter 形式：mapper XML 里通过 #{q.resolvedSize} 等属性引用它们

    public int getResolvedPage() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int getResolvedSize() {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 200);
    }

    public int getOffset() {
        return (getResolvedPage() - 1) * getResolvedSize();
    }

    /**
     * 白名单校验后的 ORDER BY 片段，非法 sortBy 一律回退到 id。
     * 必须是 JavaBean getter 形式：mapper XML 里通过 ${q.orderByClause} 引用。
     * 注意：Map.of() 返回的不可变 Map 不接受 null key，必须先判空。
     */
    public String getOrderByClause() {
        Map<String, String> whitelist = sortWhitelist();
        String column = (sortBy == null || !whitelist.containsKey(sortBy))
                ? DEFAULT_SORT_BY
                : whitelist.get(sortBy);
        String direction = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        return " ORDER BY " + column + " " + direction + ", id DESC";
    }
}
