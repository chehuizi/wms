package com.wms.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 拣货波次分页查询参数，见契约第 16.3 节。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WaveQuery extends BaseQuery {

    private Long warehouseId;

    @Override
    protected Map<String, String> sortWhitelist() {
        return Map.of(
                "id", "wv.id",
                "waveNo", "wv.wave_no",
                "status", "wv.status",
                "createdAt", "wv.created_at"
        );
    }
}
