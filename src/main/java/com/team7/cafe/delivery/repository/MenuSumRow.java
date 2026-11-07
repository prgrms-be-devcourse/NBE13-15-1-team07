package com.team7.cafe.delivery.repository;

/** A18 페이징 쿼리의 한 줄(native 쿼리의 별칭과 이름이 같아야 한다). */
public interface MenuSumRow {

    Long getMenuId();

    String getMenuName();

    Integer getUnitPrice();

    Long getTotalQuantity();

    Long getTotalAmount();
}
