package com.team7.cafe.delivery.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team7.cafe.delivery.dto.DeliveryBatchResponse;
import com.team7.cafe.delivery.dto.DeliveryItemResponse;
import com.team7.cafe.delivery.dto.PageResponse;
import com.team7.cafe.delivery.repository.DeliveryQueryRepository;
import com.team7.cafe.delivery.repository.OrderItemRow;

@Service
@Transactional(readOnly = true)
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryQueryRepository repository;

    public DeliveryService(DeliveryQueryRepository repository) {
        this.repository = repository;
    }

    /** A14. 배송일이 같은 PLACED 주문을 이메일별로 묶고, 항목을 합산한다. 이메일 오름차순. 개별 주문은 orderIds로 남긴다. */
    public List<DeliveryBatchResponse> findBatches(String shippingDate) {
        LocalDate date = DateParam.parse("shippingDate", shippingDate);
        return toBatches(date, repository.findPlacedByShippingDate(date));
    }

    /**
     * A14 페이징. 묶음(이메일 한 명)을 한 단위로 DB에서 한 페이지만 고른 뒤, 그 고객들의 행만 읽어 같은 방식으로 조립한다.
     * page는 0부터, 범위를 넘기면 content가 빈 목록이다. 잘못된 shippingDate·page·size는 400이다.
     */
    public PageResponse<DeliveryBatchResponse> findBatchesPage(String shippingDate, String page, String size) {
        LocalDate date = DateParam.parse("shippingDate", shippingDate);
        PageParam param = PageParam.parse(page, size);

        Page<String> emails = repository.findPlacedEmailsByShippingDate(date,
                PageRequest.of(param.page(), param.size()));
        List<DeliveryBatchResponse> content = emails.isEmpty()
                ? List.of()
                : toBatches(date, repository.findPlacedByShippingDateAndEmails(date, emails.getContent()));
        return new PageResponse<>(content, emails.getNumber(), emails.getSize(), emails.getTotalElements(),
                emails.getTotalPages());
    }

    /** 행을 이메일별로 묶어 배송 묶음 목록(이메일 오름차순)으로 만든다. 전체 조회와 페이징이 함께 쓴다. */
    private List<DeliveryBatchResponse> toBatches(LocalDate date, List<OrderItemRow> allRows) {
        // 1단계: 이메일별로 행을 묶는다
        Map<String, List<OrderItemRow>> rowsByEmail = new TreeMap<>();
        for (OrderItemRow row : allRows) {
            if (!rowsByEmail.containsKey(row.email())) {
                rowsByEmail.put(row.email(), new ArrayList<>());
            }
            rowsByEmail.get(row.email()).add(row);
        }

        // 2단계: 이메일마다 배송 묶음 하나를 만든다
        List<DeliveryBatchResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<OrderItemRow>> entry : rowsByEmail.entrySet()) {
            result.add(toBatch(date, entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private DeliveryBatchResponse toBatch(LocalDate date, String email, List<OrderItemRow> rows) {
        Set<Long> orderIds = new TreeSet<>();                               // 오름차순, 중복 없음
        Map<ItemKey, Integer> quantityByItem = new TreeMap<>(ItemKey.ORDER); // 메뉴 ID → 단가 → 메뉴명 순

        for (OrderItemRow row : rows) {
            orderIds.add(row.orderId());
            ItemKey key = new ItemKey(row.menuId(), row.menuName(), row.unitPrice());
            int before = quantityByItem.getOrDefault(key, 0);
            quantityByItem.put(key, before + row.quantity());
        }

        List<DeliveryItemResponse> items = new ArrayList<>();
        long totalAmount = 0;
        long totalQuantity = 0;
        for (Map.Entry<ItemKey, Integer> entry : quantityByItem.entrySet()) {
            ItemKey key = entry.getKey();
            int quantity = entry.getValue();
            long lineAmount = (long) key.unitPrice() * quantity;
            items.add(new DeliveryItemResponse(key.menuId(), key.menuName(), key.unitPrice(), quantity, lineAmount));
            totalAmount += lineAmount;
            totalQuantity += quantity;
        }

        // 같은 이메일·배송일의 PLACED 주문은 접수 시 주소가 같도록 보장되므로(ADDRESS_MISMATCH) 첫 주문의 주소를 쓴다.
        // 접수 쪽 검사를 이중으로 확인한다(동시 접수 등으로 어긋난 경우를 잡기 위함).
        warnIfAddressMismatch(date, rows);
        OrderItemRow first = rows.getFirst();
        return new DeliveryBatchResponse(date, email, first.address(), first.postcode(),
                new ArrayList<>(orderIds), orderIds.size(), items, totalAmount, totalQuantity);
    }

    /**
     * 묶음 안의 주문 주소·우편번호가 첫 주문과 다르면 경고만 남긴다. 조회는 막지 않는다.
     * 명세 규칙대로 앞뒤 공백만 제거하고 비교하며, 로그에는 개인정보(이메일·주소)를 남기지 않는다.
     */
    private void warnIfAddressMismatch(LocalDate date, List<OrderItemRow> rows) {
        OrderItemRow first = rows.getFirst();
        String address = first.address().trim();
        String postcode = first.postcode().trim();

        Set<Long> mismatchedOrderIds = new TreeSet<>();
        for (OrderItemRow row : rows) {
            if (!address.equals(row.address().trim()) || !postcode.equals(row.postcode().trim())) {
                mismatchedOrderIds.add(row.orderId());
            }
        }
        if (!mismatchedOrderIds.isEmpty()) {
            log.warn("배송 묶음 주소 불일치: shippingDate={}, 기준 주문={}, 주소가 다른 주문={}",
                    date, first.orderId(), mismatchedOrderIds);
        }
    }
}
