(function (root) {
  "use strict";
  const app = root.CoffeeLab = root.CoffeeLab || {};
  const C = app.contract, L = C.limits;
  class AppError extends Error {
    constructor(code, message, status = 0, fieldErrors = {}, uncertain = false) {
      super(message); this.name = "AppError"; this.code = code; this.status = status;
      this.fieldErrors = fieldErrors; this.uncertain = uncertain;
    }
  }
  const strip = value => value.replace(/^[\u0000-\u0020]+|[\u0000-\u0020]+$/g, "");
  function error(code, fields = {}) { const entry = C.errors[code]; return new AppError(code, entry.message, entry.status, fields); }
  function fail(fields) { const value = error("VALIDATION_ERROR", fields); value.local = true; throw value; }
  const id = value => Number.isSafeInteger(value) && value > 0;
  function validEmail(value) {
    if (typeof value !== "string" || value.length < 1 || value.length > L.emailMax) return false;
    const match = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+$/.exec(value);
    // 문자열 일부만 일치하는 경우도 거절합니다. 마지막 줄바꿈도 허용하지 않습니다.
    return match !== null && match[0] === value;
  }
  function emailInput(value) {
    if (!validEmail(value)) fail({ email: "이메일 형식을 확인해 주세요. name@example.com 형식의 1~100자로 입력하며, 공백·줄바꿈은 사용할 수 없습니다." });
    return value;
  }
  function categoryInput(input) {
    const name = input && typeof input.name === "string" ? strip(input.name) : "";
    if (!name || name.length > L.categoryNameMax) fail({ name: "카테고리명은 앞뒤 공백을 제외하고 1~30자로 입력해 주세요." });
    return { name };
  }
  function menuInput(input) {
    if (!input || typeof input !== "object" || Array.isArray(input)) fail({});
    const value = { categoryId: input.categoryId, name: typeof input.name === "string" ? strip(input.name) : "",
      price: input.price, description: typeof input.description === "string" ? strip(input.description) : "" };
    const fields = {};
    if (!id(value.categoryId)) fields.categoryId = "등록된 카테고리 ID를 선택해 주세요.";
    if (!value.name || value.name.length > L.nameMax) fields.name = "메뉴명은 앞뒤 공백을 제외하고 1~100자로 입력해 주세요.";
    if (!Number.isSafeInteger(value.price) || value.price < L.priceMin || value.price > L.priceMax) fields.price = "가격은 1~1,000,000원의 정수로 입력해 주세요.";
    if (typeof input.description !== "string" || value.description.length > L.descriptionMax) fields.description = "설명은 300자 이내의 문자열입니다. 설명이 없으면 빈 문자열을 보냅니다.";
    if (Object.keys(fields).length) fail(fields);
    return value;
  }
  function availabilityInput(input) {
    if (!input || typeof input.available !== "boolean") fail({ available: "판매 여부는 JSON의 true 또는 false로 보내 주세요." });
    return { available: input.available };
  }
  function memoInput(input) {
    if (!input || typeof input.memo !== "string" || input.memo.length > L.memoMax) fail({ memo: "메모는 200자 이내의 문자열입니다. 지우려면 빈 문자열을 보내 주세요." });
    return { memo: input.memo };
  }
  function keywordInput(value) {
    if (typeof value !== "string") fail({ keyword: "검색어를 입력해 주세요." });
    const keyword = strip(value);
    if (!keyword || keyword.length > L.keywordMax) fail({ keyword: "검색어는 앞뒤 공백을 제외하고 1~100자입니다." });
    return keyword;
  }
  function orderInput(input) {
    if (!input || typeof input !== "object" || Array.isArray(input)) fail({});
    const email = input.email;
    const address = typeof input.address === "string" ? strip(input.address) : "";
    const postcode = typeof input.postcode === "string" ? strip(input.postcode) : "";
    const fields = {};
    if (!validEmail(email)) fields.email = "이메일 형식을 확인해 주세요. name@example.com 형식의 1~100자로 입력하며, 공백·줄바꿈은 사용할 수 없습니다.";
    if (!address || address.length > L.addressMax) fields.address = "주소는 앞뒤 공백을 제외하고 1~200자입니다.";
    if (!/^[0-9]{5}$/.test(postcode)) fields.postcode = "우편번호는 숫자 0~9로 된 5자리입니다.";
    const items = Array.isArray(input.items) ? input.items : [];
    if (items.length < 1 || items.length > L.maxDistinctItems) fields.items = "서로 다른 메뉴를 1~10개 선택해 주세요.";
    const seen = new Set();
    for (const item of items) {
      if (!item || !id(item.menuId) || !Number.isInteger(item.quantity) || item.quantity < 1 || item.quantity > L.quantityMax || seen.has(item.menuId)) {
        fields.items = "메뉴 ID는 중복 없이 보내고 수량은 메뉴마다 1~20개로 입력해 주세요."; break;
      }
      seen.add(item.menuId);
    }
    if (Object.keys(fields).length) fail(fields);
    return { email, address, postcode, items: items.map(item => ({ menuId: item.menuId, quantity: item.quantity })) };
  }
  function validDate(value) {
    if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    const date = new Date(value + "T00:00:00.000Z");
    return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value && value >= "2000-01-01" && value <= "2099-12-31";
  }
  function todaySeoul() {
    return new Intl.DateTimeFormat("en-CA", { timeZone: C.timeZone, year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
  }
  const timestamp = value => typeof value === "string" && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+09:00$/.test(value);
  function ensureResponse(condition) {
    if (!condition) throw new AppError("RESPONSE_MISMATCH", "서버 응답이 24 API 명세서와 맞지 않습니다. 응답 필드와 자료형을 확인해 주세요.");
  }
  function categoryResponse(v) {
    ensureResponse(v && id(v.id) && typeof v.name === "string" && v.name.length > 0 && v.name.length <= L.categoryNameMax); return v;
  }
  function menuResponse(v) {
    ensureResponse(v && id(v.id) && id(v.categoryId) && typeof v.name === "string" && v.name.length > 0 && v.name.length <= L.nameMax &&
      typeof v.description === "string" && v.description.length <= L.descriptionMax && Number.isSafeInteger(v.price) && v.price >= 1 && v.price <= L.priceMax && typeof v.available === "boolean");
    return v;
  }
  function orderResponse(v) {
    ensureResponse(v && id(v.id) && validEmail(v.email) && typeof v.address === "string" && v.address.length > 0 && /^[0-9]{5}$/.test(v.postcode) &&
      validDate(v.shippingDate) && timestamp(v.createdAt) && Array.isArray(v.items) && v.items.length > 0 && typeof v.memo === "string" && v.memo.length <= L.memoMax);
    let total = 0; const ids = new Set();
    for (const item of v.items) {
      ensureResponse(item && id(item.menuId) && !ids.has(item.menuId) && typeof item.menuName === "string" &&
        Number.isSafeInteger(item.unitPrice) && item.unitPrice >= 1 && item.unitPrice <= L.priceMax &&
        Number.isInteger(item.quantity) && item.quantity >= 1 && item.quantity <= L.quantityMax && item.lineAmount === item.unitPrice * item.quantity);
      ids.add(item.menuId); total += item.lineAmount;
    }
    ensureResponse(v.totalAmount === total && ["PLACED", "CANCELLED"].includes(v.status) &&
      (v.status === "PLACED" ? v.cancelledAt === null : timestamp(v.cancelledAt)));
    return v;
  }
  function summaryResponse(v) {
    ensureResponse(v && id(v.id) && timestamp(v.createdAt) && validDate(v.shippingDate) && Number.isSafeInteger(v.totalAmount) && v.totalAmount > 0 && ["PLACED", "CANCELLED"].includes(v.status)); return v;
  }
  function batchResponse(v) {
    ensureResponse(v && validDate(v.shippingDate) && validEmail(v.email) && typeof v.address === "string" && /^[0-9]{5}$/.test(v.postcode) &&
      Array.isArray(v.orderIds) && v.orderIds.length > 0 && v.orderIds.every(id) && new Set(v.orderIds).size === v.orderIds.length &&
      v.orderIds.every((value, i) => i === 0 || v.orderIds[i - 1] < value) && v.orderCount === v.orderIds.length && Array.isArray(v.items) && v.items.length > 0);
    let amount = 0, quantity = 0;
    for (const item of v.items) {
      ensureResponse(item && id(item.menuId) && typeof item.menuName === "string" && Number.isSafeInteger(item.unitPrice) && item.unitPrice > 0 &&
        Number.isSafeInteger(item.quantity) && item.quantity > 0 && item.lineAmount === item.unitPrice * item.quantity);
      amount += item.lineAmount; quantity += item.quantity;
    }
    ensureResponse(v.totalAmount === amount && v.totalQuantity === quantity); return v;
  }
  function orderStatisticsResponse(v) {
    ensureResponse(v && validDate(v.date));
    for (const key of ["orderCount", "placedOrderCount", "cancelledOrderCount", "totalQuantity", "totalAmount"]) ensureResponse(Number.isSafeInteger(v[key]) && v[key] >= 0);
    ensureResponse(v.orderCount === v.placedOrderCount + v.cancelledOrderCount); return v;
  }
  function menuStatisticsResponse(v) {
    ensureResponse(v && id(v.menuId) && typeof v.menuName === "string" && Number.isSafeInteger(v.unitPrice) && v.unitPrice > 0 &&
      Number.isSafeInteger(v.totalQuantity) && v.totalQuantity > 0 && v.totalAmount === v.unitPrice * v.totalQuantity); return v;
  }
  function customerStatisticsResponse(v) { orderStatisticsResponse(v); ensureResponse(validEmail(v.email) && v.orderCount > 0); return v; }
  app.domain = { AppError, id, strip, validEmail, emailInput, categoryInput, menuInput, availabilityInput, memoInput, keywordInput, orderInput,
    validDate, todaySeoul, error, ensureResponse, categoryResponse, menuResponse, orderResponse, summaryResponse, batchResponse,
    orderStatisticsResponse, menuStatisticsResponse, customerStatisticsResponse };
  if (typeof module !== "undefined" && module.exports) module.exports = app.domain;
})(typeof window !== "undefined" ? window : globalThis);
