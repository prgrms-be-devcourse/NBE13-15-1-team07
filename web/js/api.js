(function (root) {
  "use strict";
  const app = root.CoffeeLab, D = app.domain, C = app.contract;
  function arrayOf(value, check) { D.ensureResponse(Array.isArray(value)); value.forEach(check); return value; }
  const menuPath = id => C.routes.menu.replace("{menuId}", encodeURIComponent(id));
  const categoryPath = id => C.routes.category.replace("{categoryId}", encodeURIComponent(id));
  const orderPath = id => C.routes.order.replace("{orderId}", encodeURIComponent(id));
  function dateQuery(date) { if (!D.validDate(date)) throw D.error("VALIDATION_ERROR", { date: "올바른 접수일을 선택해 주세요." }); return "?date=" + encodeURIComponent(date); }
  class ApiClient {
    constructor(baseUrl = C.apiBaseUrl, options = {}) {
      let url;
      try { url = new URL(baseUrl); } catch (_) { throw new D.AppError("INVALID_URL", "API 주소는 http://127.0.0.1:8080처럼 입력해 주세요."); }
      if (!/^https?:$/.test(url.protocol) || url.username || url.password || url.search || url.hash || !["", "/"].includes(url.pathname)) throw new D.AppError("INVALID_URL", "API 주소에는 서버 주소만 입력해 주세요. /api/v1은 붙이지 않습니다.");
      this.baseUrl = url.origin; this.fetch = options.fetch || root.fetch.bind(root); this.timeoutMs = options.timeoutMs || C.timeoutMs;
    }
    async request(path, method, expected, body, validate) {
      const controller = new AbortController(), timer = setTimeout(() => controller.abort(), this.timeoutMs);
      const mutating = !["GET", "HEAD"].includes(method);
      try {
        const options = { method, headers: { Accept: "application/json" }, credentials: "omit", signal: controller.signal };
        if (body !== undefined) { options.headers["Content-Type"] = "application/json"; options.body = JSON.stringify(body); }
        const response = await this.fetch(this.baseUrl + path, options);
        let data = null;
        if (response.status !== 204) { const text = await response.text(); if (text) { try { data = JSON.parse(text); } catch (_) {} } }
        if (response.status !== expected) {
          const structured = data && typeof data === "object" && typeof data.code === "string" && typeof data.message === "string";
          const fields = structured && data.fieldErrors && typeof data.fieldErrors === "object" && !Array.isArray(data.fieldErrors) ? data.fieldErrors : {};
          throw new D.AppError(structured ? data.code : "HTTP_ERROR", structured ? data.message : "요청을 처리하지 못했습니다. 응답과 서버 로그를 확인해 주세요.", response.status, fields, mutating && (response.status >= 500 || (response.status >= 200 && response.status < 300)));
        }
        if (expected === 204) return null;
        try { D.ensureResponse(data !== null); return validate ? validate(data) : data; }
        catch (error) { error.uncertain = mutating; throw error; }
      } catch (error) {
        if (error instanceof D.AppError) throw error;
        throw new D.AppError(controller.signal.aborted ? "TIMEOUT" : "NETWORK_ERROR",
          controller.signal.aborted ? "15초 안에 응답을 확인하지 못했습니다. 서버 상태를 확인해 주세요." : "서버에 연결하지 못했습니다. 서버 주소·실행 상태·CORS 설정을 확인해 주세요.", 0, {}, mutating);
      } finally { clearTimeout(timer); }
    }
    listCategories() { return this.request(C.routes.categories, "GET", 200, undefined, v => arrayOf(v, D.categoryResponse)); }
    getCategory(id) { return this.request(categoryPath(id), "GET", 200, undefined, D.categoryResponse); }
    createCategory(input) { return this.request(C.routes.categories, "POST", 201, D.categoryInput(input), D.categoryResponse); }
    updateCategory(id, input) { return this.request(categoryPath(id), "PUT", 200, D.categoryInput(input), D.categoryResponse); }
    deleteCategory(id) { return this.request(categoryPath(id), "DELETE", 204); }
    listMenus() { return this.request(C.routes.menus, "GET", 200, undefined, v => arrayOf(v, D.menuResponse)); }
    getMenu(id) { return this.request(menuPath(id), "GET", 200, undefined, D.menuResponse); }
    createMenu(input) { return this.request(C.routes.menus, "POST", 201, D.menuInput(input), D.menuResponse); }
    updateMenu(id, input) { return this.request(menuPath(id), "PUT", 200, D.menuInput(input), D.menuResponse); }
    deleteMenu(id) { return this.request(menuPath(id), "DELETE", 204); }
    listCategoryMenus(id) { return this.request(C.routes.categoryMenus.replace("{categoryId}", encodeURIComponent(id)), "GET", 200, undefined, v => arrayOf(v, D.menuResponse)); }
    createOrder(input) { return this.request(C.routes.orders, "POST", 201, D.orderInput(input), D.orderResponse); }
    listOrders(email) { const value = D.emailInput(email); return this.request(C.routes.orders + "?email=" + encodeURIComponent(value), "GET", 200, undefined, v => arrayOf(v, D.summaryResponse)); }
    getOrder(id) { return this.request(orderPath(id), "GET", 200, undefined, D.orderResponse); }
    cancelOrder(id) { return this.request(C.routes.cancelOrder.replace("{orderId}", encodeURIComponent(id)), "POST", 200, undefined, D.orderResponse); }
    listDeliveries(date) {
      if (!D.validDate(date)) throw D.error("VALIDATION_ERROR", { shippingDate: "올바른 배송 대상일을 선택해 주세요." });
      return this.request(C.routes.deliveries + "?shippingDate=" + encodeURIComponent(date), "GET", 200, undefined, v => arrayOf(v, D.batchResponse));
    }
    getOrderStatistics(date) { return this.request(C.routes.orderStats + dateQuery(date), "GET", 200, undefined, D.orderStatisticsResponse); }
    getMenuStatistics(date) { return this.request(C.routes.menuStats + dateQuery(date), "GET", 200, undefined, v => arrayOf(v, D.menuStatisticsResponse)); }
    listAvailableMenus() { return this.request(C.routes.availableMenus, "GET", 200, undefined, v => { arrayOf(v, D.menuResponse); D.ensureResponse(v.every(m => m.available)); return v; }); }
    searchMenus(keyword) { return this.request(C.routes.menuSearch + "?keyword=" + encodeURIComponent(D.keywordInput(keyword)), "GET", 200, undefined, v => arrayOf(v, D.menuResponse)); }
    updateMenuAvailability(id, available) { return this.request(C.routes.menuAvailability.replace("{menuId}", encodeURIComponent(id)), "PUT", 200, D.availabilityInput({ available }), D.menuResponse); }
    updateOrderMemo(id, input) { return this.request(C.routes.orderMemo.replace("{orderId}", encodeURIComponent(id)), "PUT", 200, D.memoInput(input), D.orderResponse); }
    reorder(id) { return this.request(C.routes.reorder.replace("{orderId}", encodeURIComponent(id)), "POST", 201, undefined, D.orderResponse); }
    getCustomerStatistics(date) { return this.request(C.routes.customerStats + dateQuery(date), "GET", 200, undefined, v => arrayOf(v, D.customerStatisticsResponse)); }
  }
  app.ApiClient = ApiClient;
  if (typeof module !== "undefined" && module.exports) module.exports = ApiClient;
})(typeof window !== "undefined" ? window : globalThis);
