package com.team7.cafe.delivery.service;

import java.util.Comparator;

/**
 * 메뉴 ID·주문 당시 메뉴명·단가가 모두 같을 때만 같은 항목으로 합산한다.
 * 메뉴명은 대소문자까지 문자열 그대로 비교한다(SQL GROUP BY는 collation 때문에 다르게 합칠 수 있다).
 */
record ItemKey(long menuId, String menuName, int unitPrice) {

    /** 메뉴 ID → 단가 → 메뉴명 오름차순. */
    static final Comparator<ItemKey> ORDER = Comparator
            .comparingLong(ItemKey::menuId)
            .thenComparingInt(ItemKey::unitPrice)
            .thenComparing(ItemKey::menuName);
}
