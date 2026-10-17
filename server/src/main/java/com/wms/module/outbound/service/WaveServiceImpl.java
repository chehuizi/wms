package com.wms.module.outbound.service;

import com.wms.common.BizException;
import com.wms.common.PageResult;
import com.wms.dto.WaveCreateRequest;
import com.wms.dto.WaveOrdersRequest;
import com.wms.dto.WaveQuery;
import com.wms.entity.Allocation;
import com.wms.entity.Location;
import com.wms.entity.OutboundOrder;
import com.wms.entity.PickTask;
import com.wms.entity.Wave;
import com.wms.mapper.LocationMapper;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.PickTaskMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.mapper.WaveMapper;
import com.wms.module.common.DocNoGenerator;
import com.wms.module.outbound.WaveService;
import com.wms.vo.WaveVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 拣货波次实现，见契约第 16.3 节。
 */
@Service
public class WaveServiceImpl implements WaveService {

    /** 波次状态：1=创建中 2=已释放 3=拣货中 4=已完成 5=已关闭 */
    private static final int WAVE_CREATING = 1;
    private static final int WAVE_PICKING = 3;

    /** 出库订单：3=已分配 4=拣货中 */
    private static final int ORDER_ALLOCATED = 3;
    private static final int ORDER_PICKING = 4;

    /** 分配状态：1=已锁定 -> 2=拣货中 */
    private static final int ALLOC_LOCKED = 1;
    private static final int ALLOC_PICKING = 2;

    private final WaveMapper waveMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final PickTaskMapper pickTaskMapper;
    private final LocationMapper locationMapper;
    private final WarehouseMapper warehouseMapper;
    private final DocNoGenerator docNoGenerator;

    public WaveServiceImpl(WaveMapper waveMapper,
                           OutboundOrderMapper outboundOrderMapper,
                           PickTaskMapper pickTaskMapper,
                           LocationMapper locationMapper,
                           WarehouseMapper warehouseMapper,
                           DocNoGenerator docNoGenerator) {
        this.waveMapper = waveMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.pickTaskMapper = pickTaskMapper;
        this.locationMapper = locationMapper;
        this.warehouseMapper = warehouseMapper;
        this.docNoGenerator = docNoGenerator;
    }

    @Override
    public PageResult<WaveVO> page(WaveQuery query) {
        long total = waveMapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, query.getResolvedPage(), query.getResolvedSize());
        }
        return new PageResult<>(waveMapper.selectPage(query), total,
                query.getResolvedPage(), query.getResolvedSize());
    }

    @Override
    public WaveVO getById(Long id) {
        WaveVO vo = waveMapper.selectVOById(id);
        if (vo == null) {
            throw BizException.notFound("波次不存在: " + id);
        }
        return vo;
    }

    @Override
    @Transactional
    public WaveVO create(WaveCreateRequest request) {
        if (warehouseMapper.selectById(request.getWarehouseId()) == null) {
            throw BizException.relatedInvalid("仓库不存在: " + request.getWarehouseId());
        }
        Wave wave = new Wave();
        wave.setWaveNo(docNoGenerator.next(DocNoGenerator.WAVE));
        wave.setWarehouseId(request.getWarehouseId());
        wave.setStatus(WAVE_CREATING);
        wave.setOrderCount(0);
        wave.setOperator(request.getOperator());
        waveMapper.insert(wave);
        return getById(wave.getId());
    }

    @Override
    @Transactional
    public WaveVO addOrders(Long id, WaveOrdersRequest request) {
        requireCreating(id);
        Set<Long> added = new HashSet<>();
        for (Long outboundId : request.getOutboundIds()) {
            OutboundOrder order = outboundOrderMapper.selectById(outboundId);
            if (order == null) {
                throw BizException.relatedInvalid("出库订单不存在: " + outboundId);
            }
            // 只能加入 status = 3 已分配的订单
            if (order.getStatus() == null || order.getStatus() != ORDER_ALLOCATED) {
                throw BizException.orderStatusNotAllowed(
                        "只有已分配的订单可以加入波次: " + order.getOrderNo() + "，当前状态 " + order.getStatus());
            }
            if (added.add(outboundId)) {
                waveMapper.insertWaveOrder(id, outboundId);
            }
        }
        waveMapper.refreshOrderCount(id);
        return getById(id);
    }

    @Override
    @Transactional
    public WaveVO release(Long id) {
        Wave wave = requireCreating(id);

        List<Long> outboundIds = waveMapper.selectWaveOutboundIds(id);
        if (outboundIds.isEmpty()) {
            throw BizException.statusInvalid("波次中没有订单，不能释放: " + wave.getWaveNo());
        }
        for (Long outboundId : outboundIds) {
            OutboundOrder order = outboundOrderMapper.selectById(outboundId);
            if (order == null) {
                throw BizException.relatedInvalid("波次关联的订单不存在: " + outboundId);
            }
            if (order.getStatus() == null || order.getStatus() != ORDER_ALLOCATED) {
                throw BizException.orderStatusNotAllowed(
                        "订单 " + order.getOrderNo() + " 当前状态 " + order.getStatus() + " 不能进入拣货");
            }
            if (!order.getWarehouseId().equals(wave.getWarehouseId())) {
                throw BizException.relatedInvalid("订单与波次不在同一仓库: " + order.getOrderNo());
            }
        }

        // 收集全部待拣分配，按库位编码升序（同库位相邻）排序，sequence 递增
        List<Candidate> candidates = collectCandidates(outboundIds);
        candidates.sort(Comparator.comparing(Candidate::locationCode).thenComparing(c -> c.allocation().getId()));

        waveMapper.markReleased(id, wave.getOperator());
        int sequence = 1;
        for (Candidate candidate : candidates) {
            Allocation alloc = candidate.allocation();
            PickTask task = new PickTask();
            task.setTaskNo(docNoGenerator.next(DocNoGenerator.PICK_TASK));
            task.setWaveId(id);
            task.setOutboundId(candidate.outboundId());
            task.setAllocationId(alloc.getId());
            task.setSkuId(alloc.getSkuId());
            task.setFromLocationId(alloc.getLocationId());
            task.setSequence(sequence++);
            task.setRequiredQty(alloc.getAllocatedQty());
            task.setPickedQty(BigDecimal.ZERO);
            task.setStatus(1);
            pickTaskMapper.insert(task);
            outboundOrderMapper.markAllocationStatus(alloc.getId(), ALLOC_PICKING);
        }
        for (Long outboundId : outboundIds) {
            outboundOrderMapper.updateStatus(outboundId, ORDER_PICKING);
        }
        waveMapper.refreshOrderCount(id);
        waveMapper.updateStatus(id, WAVE_PICKING);
        return getById(id);
    }

    private List<Candidate> collectCandidates(List<Long> outboundIds) {
        List<Candidate> candidates = new ArrayList<>();
        Set<Long> locationIds = new HashSet<>();
        for (Long outboundId : outboundIds) {
            for (Allocation alloc : outboundOrderMapper.selectAllocationsByStatus(outboundId, List.of(ALLOC_LOCKED))) {
                candidates.add(new Candidate(outboundId, alloc, null));
                locationIds.add(alloc.getLocationId());
            }
        }
        Map<Long, String> codes = locationMapper.selectByIds(new ArrayList<>(locationIds)).stream()
                .collect(Collectors.toMap(Location::getId, l -> l.getCode() == null ? "" : l.getCode()));
        return candidates.stream()
                .map(c -> new Candidate(c.outboundId(), c.allocation(), codes.getOrDefault(c.allocation().getLocationId(), "")))
                .toList();
    }

    private Wave requireCreating(Long id) {
        Wave wave = waveMapper.selectForUpdate(id);
        if (wave == null) {
            throw BizException.notFound("波次不存在: " + id);
        }
        if (wave.getStatus() != null && wave.getStatus() != WAVE_CREATING) {
            throw BizException.orderStatusNotAllowed(
                    "只有创建中的波次可以操作，当前状态: " + wave.getStatus());
        }
        return wave;
    }

    /** 待拣分配：所属订单 + 分配记录 + 库位编码（用于按库位编码升序排序） */
    private record Candidate(Long outboundId, Allocation allocation, String locationCode) {
    }
}
