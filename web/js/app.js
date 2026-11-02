(function (root) {
  "use strict";
  const app = root.CoffeeLab;
  const D = app.domain;
  const C = app.contract;
  const $ = id => document.getElementById(id);
  const won = value => new Intl.NumberFormat("ko-KR").format(value) + "원";
  const params = new URLSearchParams(root.location.search);
  const localFile = root.location.protocol === "file:";
  const state = { mode: localFile || params.get("mode") === "demo" ? "demo" : "api", baseUrl: params.get("apiBase") || C.apiBaseUrl, client: null, menus: [], catalogMenus: [], catalogSeq: 0, menusLoaded: false, cart: new Map(), busy: false, uncertain: false, editingId: null, deleting: null, lastOrder: null, menuSeq: 0 };

  function isPreview() { return state.mode === "demo"; }
  function syncFormControls() {
    const locked = state.busy || isPreview();
    document.querySelectorAll("#panel-shop form input, #panel-shop form select, #panel-shop form textarea, #panel-shop form button, #panel-orders form input, #panel-orders form button, #panel-admin form input, #panel-admin form select, #panel-admin form textarea, #panel-admin form button, #order-detail form textarea, #order-detail form button").forEach(control => { control.disabled = locked; });
  }

  function node(tag, text, className) {
    const value = document.createElement(tag);
    if (text !== undefined) value.textContent = text;
    if (className) value.className = className;
    return value;
  }
  function button(text, run, className = "button subtle small") {
    const value = node("button", text, className);
    value.type = "button";
    value.disabled = isPreview() && !value.classList.contains("preview-view");
    value.addEventListener("click", event => { if (!isPreview() || value.classList.contains("preview-view")) run(event); });
    return value;
  }
  function announce(text, error = false) {
    const message = $("global-message");
    message.textContent = text;
    message.className = error ? "notice error" : "notice";
    message.hidden = !text;
  }
  function feedback(id, text, success = false) {
    $(id).textContent = text;
    $(id).classList.toggle("success", success);
  }
  function errorText(error) {
    const fields = error.fieldErrors ? Object.values(error.fieldErrors).filter(value => typeof value === "string") : [];
    const detail = fields.length ? "\n" + [...new Set(fields)].join("\n") : "";
    return (error.local ? "화면 입력 확인: " : "") + error.message + detail;
  }
  function setBusy(value) {
    state.busy = value; syncFormControls();
    for (const id of ["save-menu", "lookup-submit", "delivery-submit", "open-settings", "reload-menus", "confirm-delete", "save-category", "reload-categories", "category-filter-submit", "statistics-submit", "confirm-category-delete", "menu-search-submit", "clear-search", "only-available"]) $(id).disabled = value || (isPreview() && id !== "open-settings");
    document.querySelectorAll('[role="tab"]').forEach(tab => { tab.disabled = value; });
    renderCart();
    document.querySelectorAll(".add-menu, .admin-menu-row button, .category-row button, .order-row button, .order-action").forEach(control => { control.disabled = state.busy || (isPreview() && !control.classList.contains("preview-view")) || control.dataset.unavailable === "true" || (control.dataset.createsOrder === "true" && state.uncertain); });
  }
  function selectTab(name) {
    if (state.busy) return;
    for (const key of ["shop", "orders", "admin"]) {
      const selected = key === name;
      $("tab-" + key).setAttribute("aria-selected", String(selected));
      $("tab-" + key).tabIndex = selected ? 0 : -1;
      $("panel-" + key).hidden = !selected;
    }
  }
  function renderConnection(online = false) {
    const badge = $("connection-status");
    badge.className = "connection " + (state.mode === "demo" ? "demo" : online ? "online" : "");
    badge.textContent = isPreview() ? "고정 예시 화면" : online ? "API 연결됨" : "API 연결 확인 필요";
    $("mode-banner").hidden = state.mode !== "demo";
    $("mode-banner").textContent = "고정 예시 화면입니다. 탭과 예시 주문 상세를 열어볼 수 있습니다. 검색·수정·주문·조회 기능은 연결 설정에서 실제 API에 연결한 뒤 사용할 수 있습니다.";
  }
  function makeClient() {
    state.client = isPreview() ? null : new app.ApiClient(state.baseUrl);
    setBusy(false); renderConnection(false);
    $("delivery-date").value = isPreview() ? app.previewSnapshot.date : D.todaySeoul();
  }
  async function loadCatalog() {
    if (isPreview()) return;
    const seq = ++state.catalogSeq, client = state.client;
    const raw = $("menu-keyword").value, onlyAvailable = $("only-available").checked;
    const menus = raw !== "" ? await client.searchMenus(raw) : onlyAvailable ? await client.listAvailableMenus() : await client.listMenus();
    if (seq !== state.catalogSeq || client !== state.client) return;
    state.catalogMenus = onlyAvailable ? menus.filter(menu => menu.available) : menus;
    $("menu-status").hidden = state.catalogMenus.length > 0;
    $("menu-status").className = "empty-state";
    $("menu-status").textContent = raw !== "" ? "검색 조건에 맞는 메뉴가 없습니다. 메뉴명은 대소문자를 구분합니다." : onlyAvailable ? "현재 판매 중인 메뉴가 없습니다." : "등록된 메뉴가 없습니다.";
    renderMenus();
  }
  function renderPreview() {
    const sample = app.previewSnapshot;
    state.menuSeq++; state.catalogSeq++;
    state.menus = sample.menus; state.catalogMenus = sample.menus; state.menusLoaded = true;
    state.cart.clear(); state.lastOrder = null; state.uncertain = false;
    $("menu-status").hidden = true; $("menu-status").className = "empty-state";
    $("order-success").hidden = true;
    $("lookup-email").value = sample.order.email;
    $("customer-email").value = sample.order.email;
    $("customer-address").value = sample.order.address;
    $("customer-postcode").value = sample.order.postcode;
    $("delivery-date").value = sample.date;
    renderMenus(); renderAdminMenus(); renderOrders(sample.orderSummaries); renderDeliveries(sample.deliveryBatches);
    feedback("lookup-feedback", "고정된 예시 주문입니다. 실제 조회는 API 연결 후 사용할 수 있습니다.");
    feedback("delivery-feedback", "고정된 예시 배송 묶음입니다.");
    setBusy(false);
  }

  async function loadMenus() {
    if (isPreview()) { renderPreview(); return; }
    const seq = ++state.menuSeq, client = state.client;
    state.menusLoaded = false; renderCart(); $("reload-menus").disabled = true;
    try {
      const menus = await client.listMenus();
      if (seq !== state.menuSeq || client !== state.client) return;
      state.menus = menus; state.menusLoaded = true;
      const activeIds = new Set(menus.filter(item => item.available).map(item => item.id));
      let removed = 0;
      for (const id of state.cart.keys()) if (!activeIds.has(id)) { state.cart.delete(id); removed++; }
      if (removed) announce("삭제되었거나 판매 중지된 메뉴를 장바구니에서 뺐습니다.");
      renderAdminMenus(); renderCart(); renderConnection(true);
      try { await loadCatalog(); } catch (error) {
        if (!error.local) throw error;
        $("menu-grid").replaceChildren(); $("menu-status").hidden = false; $("menu-status").className = "empty-state error"; $("menu-status").textContent = errorText(error);
      }
      document.dispatchEvent(new CustomEvent("coffeelab:menus-changed"));
    } catch (error) {
      if (seq !== state.menuSeq || client !== state.client) return;
      $("menu-grid").replaceChildren();
      $("menu-status").hidden = false; $("menu-status").className = "empty-state error"; $("menu-status").textContent = errorText(error);
      renderConnection(false); throw error;
    } finally { if (seq === state.menuSeq) $("reload-menus").disabled = state.busy; }
  }
  async function refreshCatalog() {
    if (state.busy || isPreview()) return;
    setBusy(true);
    try { await loadCatalog(); announce(""); }
    catch (error) { $("menu-grid").replaceChildren(); $("menu-status").hidden = false; $("menu-status").textContent = errorText(error); $("menu-status").className = "empty-state error"; }
    finally { setBusy(false); }
  }
  function renderMenus() {
    const grid = $("menu-grid"); grid.replaceChildren();
    $("menu-count").textContent = state.catalogMenus.length + "종";
    for (const menu of state.catalogMenus) {
      const fragment = $("menu-card-template").content.cloneNode(true);
      fragment.querySelector(".menu-card-name").textContent = menu.name;
      fragment.querySelector(".menu-card-description").textContent = menu.description || "";
      fragment.querySelector(".menu-card-price").textContent = won(menu.price);
      const status = node("span", menu.available ? "판매 중" : "판매 중지", "sale-badge " + (menu.available ? "on" : "off"));
      fragment.querySelector(".menu-card-body").prepend(status);
      fragment.querySelector(".menu-card").dataset.menuId = menu.id;
      const add = fragment.querySelector(".add-menu");
      add.dataset.unavailable = String(!menu.available); add.disabled = state.busy || isPreview() || !menu.available;
      if (!menu.available) add.textContent = "판매 중지";
      add.setAttribute("aria-label", menu.name + (menu.available ? " 담기" : " 판매 중지"));
      add.addEventListener("click", () => {
        if (state.busy || isPreview() || !menu.available) return;
        const quantity = state.cart.get(menu.id) || 0;
        if (quantity >= C.limits.quantityMax) return announce("한 메뉴는 최대 20개까지 담을 수 있습니다.", true);
        if (!quantity && state.cart.size >= C.limits.maxDistinctItems) return announce("서로 다른 메뉴는 최대 10개까지 담을 수 있습니다.", true);
        state.cart.set(menu.id, quantity + 1); $("order-success").hidden = true; renderCart(); announce(menu.name + " 원두를 담았습니다.");
      });
      grid.appendChild(fragment);
    }
  }
  function renderCart() {
    const list = $("cart-items");
    list.replaceChildren();
    let total = 0;
    let count = 0;
    for (const [id, quantity] of state.cart) {
      const menu = state.menus.find(value => value.id === id);
      if (!menu) continue;
      total += menu.price * quantity;
      count += quantity;
      const item = node("li", undefined, "cart-item");
      const title = node("div", undefined, "cart-item-title");
      title.append(node("span", menu.name));
      const remove = button("×", () => { if (!state.busy) { state.cart.delete(id); renderCart(); } }, "icon-button");
      remove.setAttribute("aria-label", menu.name + " 장바구니에서 빼기");
      remove.disabled = state.busy;
      title.append(remove);
      const bottom = node("div", undefined, "cart-item-bottom");
      const stepper = node("div", undefined, "quantity");
      const minus = button("−", () => { if (!state.busy) { if (quantity === 1) state.cart.delete(id); else state.cart.set(id, quantity - 1); renderCart(); } }, "");
      const plus = button("+", () => { if (!state.busy && quantity < C.limits.quantityMax) { state.cart.set(id, quantity + 1); renderCart(); } }, "");
      minus.setAttribute("aria-label", menu.name + " 수량 줄이기");
      plus.setAttribute("aria-label", menu.name + " 수량 늘리기");
      minus.disabled = state.busy;
      plus.disabled = state.busy || quantity === C.limits.quantityMax;
      stepper.append(minus, node("span", quantity), plus);
      bottom.append(stepper, node("strong", won(menu.price * quantity)));
      item.append(title, bottom);
      list.append(item);
    }
    $("cart-count").textContent = count + "개";
    $("cart-total").textContent = won(total);
    $("cart-empty").hidden = count > 0;
    $("place-order").disabled = state.busy || isPreview() || state.uncertain || !state.menusLoaded || count === 0;
    $("place-order").textContent = state.busy ? "처리 중…" : "주문하기";
    $("uncertain-actions").hidden = !state.uncertain;
  }
  function resetEditor() {
    state.editingId = null;
    $("menu-form").reset();
    $("editor-heading").textContent = "새 메뉴 등록";
    $("save-menu").textContent = "메뉴 등록";
    $("cancel-edit").hidden = true;
    feedback("menu-feedback", "");
  }
  function renderAdminMenus() {
    const list = $("admin-menu-list"); list.replaceChildren();
    if (!state.menus.length) return list.append(node("p", "아직 등록한 메뉴가 없습니다.", "muted"));
    for (const menu of state.menus) {
      const row = node("article", undefined, "admin-menu-row"); row.dataset.menuId = menu.id;
      const description = node("div", undefined, "grow");
      description.append(node("h3", menu.name), node("p", "메뉴 #" + menu.id + " · " + won(menu.price)),
        node("span", menu.available ? "판매 중" : "판매 중지", "sale-badge " + (menu.available ? "on" : "off")));
      const actions = node("div", undefined, "button-row");
      const edit = button("수정", async () => {
        if (state.busy) return; setBusy(true);
        try {
          const current = await state.client.getMenu(menu.id); state.editingId = current.id;
          $("menu-name").value = current.name; $("menu-price").value = current.price; $("menu-description").value = current.description; $("menu-category").value = String(current.categoryId);
          $("editor-heading").textContent = "메뉴 #" + current.id + " 수정"; $("save-menu").textContent = "메뉴 수정"; $("cancel-edit").hidden = false;
          feedback("menu-feedback", "수정할 내용을 입력한 뒤 메뉴 수정을 눌러주세요.", true); $("menu-name").focus();
        } catch (error) { feedback("menu-feedback", errorText(error)); } finally { setBusy(false); }
      });
      edit.setAttribute("aria-label", menu.name + " 수정");
      const availability = button(menu.available ? "판매 중지" : "판매 재개", async () => {
        if (state.busy) return; setBusy(true);
        try {
          await state.client.updateMenuAvailability(menu.id, !menu.available);
          announce("‘" + menu.name + "’ 메뉴를 " + (menu.available ? "판매 중지했습니다." : "다시 판매합니다."));
          await loadMenus();
        } catch (error) { announce(errorText(error) + (error.uncertain ? " 판매 상태를 다시 조회한 뒤 확인해 주세요." : ""), true); }
        finally { setBusy(false); }
      }, "button outline small");
      availability.setAttribute("aria-label", menu.name + (menu.available ? " 판매 중지" : " 판매 재개"));
      const remove = button("삭제", () => {
        if (state.busy) return; state.deleting = menu; $("delete-description").textContent = "‘" + menu.name + "’ 메뉴를 삭제합니다."; $("delete-dialog").showModal();
      });
      remove.setAttribute("aria-label", menu.name + " 삭제");
      actions.append(edit, availability, remove); row.append(description, actions); list.append(row);
    }
  }
  function renderOrderDetail(order) {
    const area = $("order-detail"); area.replaceChildren(); $("detail-heading").textContent = "주문 #" + order.id;
    if (isPreview()) area.append(node("p", "고정 예시 주문입니다. 메모 저장·취소·재주문은 실제 API 연결 후 사용할 수 있습니다.", "notice"));
    const meta = node("dl", undefined, "detail-meta");
    for (const [label, value] of [["주문 상태", order.status === "CANCELLED" ? "취소 완료" : "접수"], ["취소 시각", order.cancelledAt || "—"], ["접수 시각", order.createdAt], ["배송 대상일", order.shippingDate], ["이메일", order.email], ["배송 주소", order.address], ["우편번호", order.postcode]]) meta.append(node("dt", label), node("dd", value));
    area.append(meta);
    for (const item of order.items) {
      const row = node("div", undefined, "detail-item");
      row.append(node("span", item.menuName + "\n" + won(item.unitPrice) + " × " + item.quantity + "개"), node("strong", won(item.lineAmount))); area.append(row);
    }
    area.append(node("p", "총 " + won(order.totalAmount), "detail-amount"));
    const memoForm = node("form", undefined, "memo-form"); memoForm.id = "memo-form"; memoForm.noValidate = true;
    const label = node("label", "주문 내역 메모"); label.htmlFor = "order-memo";
    const memo = node("textarea"); memo.id = "order-memo"; memo.maxLength = C.limits.memoMax; memo.rows = 3; memo.value = order.memo;
    const counter = node("p", memo.value.length + "/200자", "hint"); counter.id = "memo-counter";
    memo.addEventListener("input", () => { counter.textContent = memo.value.length + "/200자"; });
    const memoFeedback = node("p", "", "form-feedback"); memoFeedback.id = "memo-feedback"; memoFeedback.setAttribute("aria-live", "polite");
    const save = node("button", "메모 저장", "button outline order-action"); save.type = "submit";
    memoForm.append(label, memo, counter, node("p", "최대 200자이며, 비우고 저장하면 메모를 삭제합니다. 주문 금액·상태는 바뀌지 않습니다.", "hint"), memoFeedback, save);
    memoForm.addEventListener("submit", async event => {
      event.preventDefault(); if (state.busy) return; setBusy(true);
      try { const saved = await state.client.updateOrderMemo(order.id, { memo: memo.value }); renderOrderDetail(saved); feedback("memo-feedback", "메모를 저장했습니다.", true); }
      catch (error) { feedback("memo-feedback", errorText(error) + (error.uncertain ? " 주문 상세를 다시 조회해 저장 여부를 확인해 주세요." : "")); }
      finally { setBusy(false); }
    });
    area.append(memoForm);
    if (order.status === "PLACED") {
      area.append(node("p", "이 주문의 배송 대상일 14시 전까지 취소할 수 있습니다. 마감 여부는 서버가 확인합니다.", "hint"));
      const cancelFeedback = node("p", "", "form-feedback"); cancelFeedback.id = "cancel-feedback"; cancelFeedback.setAttribute("aria-live", "polite"); area.append(cancelFeedback);
      area.append(button("이 주문 취소하기", () => cancelOrder(order.id), "button danger order-action order-cancel"));
    }
    const reorderFeedback = node("p", "", "form-feedback"); reorderFeedback.id = "reorder-feedback"; reorderFeedback.setAttribute("aria-live", "polite");
    const repeat = button("현재 가격으로 다시 주문", () => repeatOrder(order), "button primary order-action order-reorder");
    repeat.dataset.createsOrder = "true"; repeat.disabled = state.busy || state.uncertain;
    area.append(node("p", "원래 상품·수량·이메일·배송지로 새 주문을 접수합니다. 현재 메뉴명·가격과 새 접수 시각을 사용하며 메모는 복사하지 않습니다.", "hint"), reorderFeedback, repeat);
    if (state.uncertain) {
      area.append(node("p", "앞 요청의 접수 여부를 주문 내역에서 확인한 뒤 다시 주문해 주세요.", "hint"));
      area.append(button("주문 내역 확인하기", () => { $("detail-dialog").close(); $("lookup-email").value = order.email; selectTab("orders"); $("lookup-email").focus(); }, "button outline order-action"));
      area.append(button("내역 확인 후 주문 버튼 다시 사용", () => { if (state.busy) return; state.uncertain = false; renderOrderDetail(order); renderCart(); }, "text-button"));
    }
  }
  async function showOrder(id) {
    if (state.busy) return; setBusy(true);
    try { const order = isPreview() ? app.previewSnapshot.order : await state.client.getOrder(id); renderOrderDetail(order); if (!$("detail-dialog").open) $("detail-dialog").showModal(); }
    catch (error) { announce(errorText(error), true); } finally { setBusy(false); }
  }
  async function repeatOrder(order) {
    if (state.busy || state.uncertain) return; setBusy(true);
    try {
      const saved = await state.client.reorder(order.id); state.lastOrder = saved; $("lookup-email").value = saved.email;
      $("delivery-date").value = saved.shippingDate; $("success-title").textContent = "주문 #" + saved.id + " 접수 완료";
      $("success-description").textContent = saved.shippingDate + " 배송 대상 · 최종 주문 금액 " + won(saved.totalAmount); $("order-success").hidden = false;
      $("delivery-results").replaceChildren(); document.dispatchEvent(new CustomEvent("coffeelab:orders-changed"));
      renderOrderDetail(saved); announce("새 주문 #" + saved.id + "을 접수했습니다. " + saved.shippingDate + " 배송 대상 · " + won(saved.totalAmount));
      try { renderOrders(await state.client.listOrders(saved.email)); } catch (_) { announce("새 주문 #" + saved.id + "은 접수됐습니다. 주문 조회로 목록을 다시 확인해 주세요.", true); }
    } catch (error) {
      state.uncertain = Boolean(error.uncertain);
      feedback("reorder-feedback", errorText(error) + (error.uncertain ? " 새 주문의 접수 여부를 주문 조회에서 확인하세요. 자동으로 다시 요청하지 않습니다." : ""));
      if (state.uncertain) {
        const message = $("reorder-feedback").textContent; renderOrderDetail(order); feedback("reorder-feedback", message);
      }
    } finally { setBusy(false); }
  }
  async function cancelOrder(id) {
    if (state.busy) return;
    setBusy(true);
    try {
      await state.client.cancelOrder(id);
      $("detail-dialog").close();
      $("delivery-results").replaceChildren();
      document.dispatchEvent(new CustomEvent("coffeelab:orders-changed"));
      announce("주문을 취소했습니다. 주문 내역은 보존되며 배송 묶음과 수량·금액 합산에서 제외됩니다.");
      try { if (D.validEmail($("lookup-email").value)) renderOrders(await state.client.listOrders($("lookup-email").value)); }
      catch (_) { announce("주문 취소는 완료됐습니다. 목록을 갱신하지 못했으니 주문 조회로 상태를 확인해 주세요.", true); }
    } catch (error) {
      const message = errorText(error) + (error.uncertain ? " 취소 완료 여부를 주문 상세에서 확인한 뒤 다시 요청해 주세요." : "");
      if ($("cancel-feedback")) feedback("cancel-feedback", message);
      announce(message, true);
    }
    finally { setBusy(false); }
  }
  function renderOrders(orders) {
    const area = $("order-results");
    area.replaceChildren();
    if (!orders.length) return area.append(node("p", "이 이메일로 접수한 주문이 없습니다.", "empty-state"));
    for (const order of orders) {
      const row = node("article", undefined, "order-row");
      const text = node("div", undefined, "grow");
      text.append(node("h3", "주문 #" + order.id), node("p", order.createdAt + " 접수 · " + order.shippingDate + " 배송 대상 · " + (order.status === "CANCELLED" ? "취소 완료" : "접수")));
      row.append(text, node("strong", won(order.totalAmount), "amount"), button("상세 보기", () => showOrder(order.id), "button outline small preview-view"));
      area.append(row);
    }
  }
  function renderDeliveries(groups) {
    const area = $("delivery-results");
    area.replaceChildren();
    if (!groups.length) return area.append(node("p", "선택한 날짜의 배송 대상 주문이 없습니다.", "empty-state"));
    for (const group of groups) {
      const card = node("article", undefined, "delivery-card");
      card.append(node("h3", group.email), node("p", group.address + " (" + group.postcode + ")"), node("p", "주문 " + group.orderCount + "건 · 주문번호 " + group.orderIds.join(", ")));
      const items = node("ul");
      for (const item of group.items) items.append(node("li", item.menuName + " · " + won(item.unitPrice) + " × " + item.quantity + "개 = " + won(item.lineAmount)));
      const total = node("div", undefined, "delivery-total");
      total.append(node("span", "총 " + group.totalQuantity + "개"), node("strong", won(group.totalAmount)));
      card.append(items, total);
      area.append(card);
    }
  }
  document.addEventListener("submit", event => {
    if (isPreview() && event.target.id !== "settings-form") {
      event.preventDefault(); event.stopImmediatePropagation();
      announce("고정 예시 화면에서는 요청을 실행하지 않습니다. 연결 설정에서 실제 API에 연결해 주세요.");
    }
  }, true);
  for (const key of ["shop", "orders", "admin"]) $("tab-" + key).addEventListener("click", () => selectTab(key));
  document.querySelector('[role="tablist"]').addEventListener("keydown", event => {
    if (!event.target.matches('[role="tab"]') || state.busy) return;
    const keys = ["shop", "orders", "admin"];
    const current = keys.findIndex(key => "tab-" + key === event.target.id);
    let next = current;
    if (event.key === "ArrowRight") next = (current + 1) % keys.length;
    else if (event.key === "ArrowLeft") next = (current + keys.length - 1) % keys.length;
    else if (event.key === "Home") next = 0;
    else if (event.key === "End") next = keys.length - 1;
    else return;
    event.preventDefault(); selectTab(keys[next]); $("tab-" + keys[next]).focus();
  });
  $("reload-menus").addEventListener("click", () => loadMenus().catch(error => announce(errorText(error), true)));
  $("menu-search-form").addEventListener("submit", event => { event.preventDefault(); refreshCatalog(); });
  $("clear-search").addEventListener("click", () => { $("menu-keyword").value = ""; refreshCatalog(); });
  $("only-available").addEventListener("change", refreshCatalog);
  $("cancel-edit").addEventListener("click", resetEditor);
  $("checkout-form").addEventListener("submit", async event => {
    event.preventDefault();
    if (state.busy || state.uncertain || !state.menusLoaded || !state.cart.size) return;
    setBusy(true); feedback("checkout-feedback", "");
    try {
      const input = { email: $("customer-email").value, address: $("customer-address").value, postcode: $("customer-postcode").value, items: [...state.cart].map(([menuId, quantity]) => ({ menuId, quantity })) };
      const saved = await state.client.createOrder(input);
      state.lastOrder = saved;
      document.dispatchEvent(new CustomEvent("coffeelab:orders-changed"));
      state.cart.clear();
      $("lookup-email").value = saved.email;
      $("delivery-date").value = saved.shippingDate;
      $("success-title").textContent = "주문 #" + saved.id + " 접수 완료";
      $("success-description").textContent = saved.shippingDate + " 배송 대상 · 최종 주문 금액 " + won(saved.totalAmount);
      $("order-success").hidden = false;
      try { await loadMenus(); }
      catch (_) { announce("주문은 접수됐습니다. (#" + saved.id + ") 원두 목록을 갱신하지 못했으니 같은 주문을 다시 보내지 마세요.", true); }
    } catch (error) {
      state.uncertain = Boolean(error.uncertain);
      feedback("checkout-feedback", errorText(error) + (state.uncertain ? "\n주문이 이미 접수됐을 수 있습니다. 같은 주문을 다시 보내기 전에 주문 내역이나 DB를 확인해 주세요." : ""));
    } finally { setBusy(false); }
  });
  $("open-last-order").addEventListener("click", () => { if (state.lastOrder) showOrder(state.lastOrder.id); });
  $("check-uncertain").addEventListener("click", () => { $("lookup-email").value = $("customer-email").value; selectTab("orders"); $("lookup-email").focus(); });
  $("acknowledge-uncertain").addEventListener("click", () => { state.uncertain = false; feedback("checkout-feedback", "접수 내역을 확인한 뒤에만 다시 주문해 주세요."); renderCart(); });
  $("lookup-form").addEventListener("submit", async event => {
    event.preventDefault(); if (state.busy) return; setBusy(true); feedback("lookup-feedback", "");
    try { const orders = await state.client.listOrders($("lookup-email").value); renderOrders(orders); feedback("lookup-feedback", "주문 " + orders.length + "건을 확인했습니다.", true); }
    catch (error) { $("order-results").replaceChildren(); feedback("lookup-feedback", errorText(error)); }
    finally { setBusy(false); }
  });
  $("menu-form").addEventListener("submit", async event => {
    event.preventDefault(); if (state.busy) return; setBusy(true); feedback("menu-feedback", "");
    try {
      const input = { name: $("menu-name").value, price: Number($("menu-price").value), description: $("menu-description").value, categoryId: Number($("menu-category").value) };
      const saved = state.editingId === null ? await state.client.createMenu(input) : await state.client.updateMenu(state.editingId, input);
      resetEditor(); feedback("menu-feedback", "메뉴를 저장했습니다. (#" + saved.id + ")", true);
      try { await loadMenus(); } catch (_) { announce("메뉴 저장은 완료됐지만 목록을 갱신하지 못했습니다. 새로고침으로 결과를 확인해 주세요.", true); }
    } catch (error) { feedback("menu-feedback", errorText(error) + (error.uncertain ? "\n저장 여부를 목록이나 DB에서 확인한 뒤 다시 요청해 주세요." : "")); }
    finally { setBusy(false); }
  });
  $("confirm-delete").addEventListener("click", async () => {
    if (state.busy || !state.deleting) return;
    const menu = state.deleting; setBusy(true);
    try {
      await state.client.deleteMenu(menu.id);
      $("delete-dialog").close();
      if (state.editingId === menu.id) resetEditor();
      announce("‘" + menu.name + "’ 메뉴를 삭제했습니다.");
      try { await loadMenus(); } catch (_) { announce("메뉴 삭제는 완료됐지만 목록을 갱신하지 못했습니다. 새로고침으로 결과를 확인해 주세요.", true); }
    } catch (error) { $("delete-dialog").close(); announce(errorText(error), true); }
    finally { state.deleting = null; setBusy(false); }
  });
  $("cancel-delete").addEventListener("click", () => { state.deleting = null; $("delete-dialog").close(); });
  $("delivery-form").addEventListener("submit", async event => {
    event.preventDefault(); if (state.busy) return; setBusy(true); feedback("delivery-feedback", "");
    try { const groups = await state.client.listDeliveries($("delivery-date").value); renderDeliveries(groups); feedback("delivery-feedback", "배송 묶음 " + groups.length + "개를 확인했습니다.", true); }
    catch (error) { $("delivery-results").replaceChildren(); feedback("delivery-feedback", errorText(error)); }
    finally { setBusy(false); }
  });
  $("open-settings").addEventListener("click", () => { $("connection-mode").value = state.mode; $("api-base").value = state.baseUrl; $("api-base").disabled = isPreview(); feedback("settings-feedback", ""); $("settings-dialog").showModal(); });
  $("connection-mode").addEventListener("change", () => { $("api-base").disabled = $("connection-mode").value === "demo"; });
  $("close-settings").addEventListener("click", () => $("settings-dialog").close());
  $("close-detail").addEventListener("click", () => $("detail-dialog").close());
  $("settings-form").addEventListener("submit", async event => {
    event.preventDefault();
    const nextMode = $("connection-mode").value;
    if (localFile && nextMode === "api") return feedback("settings-feedback", "API 연결은 파일을 직접 연 상태에서 사용할 수 없습니다. 명세서의 HTTP 서버 실행 명령으로 열어주세요.");
    try {
      const validated = nextMode === "api" ? new app.ApiClient($("api-base").value.trim()) : { baseUrl: state.baseUrl };
      if (state.mode === nextMode && state.baseUrl === validated.baseUrl) { $("settings-dialog").close(); return; }
      state.mode = nextMode; state.baseUrl = validated.baseUrl; state.cart.clear(); state.lastOrder = null; state.uncertain = false; state.menus = []; state.catalogMenus = []; $("menu-search-form").reset(); $("only-available").checked = true;
      state.menusLoaded = false;
      $("order-success").hidden = true; $("order-results").replaceChildren(); $("delivery-results").replaceChildren(); $("menu-grid").replaceChildren(); $("admin-menu-list").replaceChildren();
      for (const id of ["checkout-form", "lookup-form", "delivery-form", "statistics-form"]) $(id).reset();
      $("detail-dialog").close(); $("delete-dialog").close(); $("category-delete-dialog").close(); resetEditor();
      feedback("checkout-feedback", ""); feedback("lookup-feedback", ""); feedback("delivery-feedback", "");
      makeClient(); renderCart(); document.dispatchEvent(new CustomEvent("coffeelab:connection-changed")); $("settings-dialog").close(); announce("연결 설정을 적용했습니다. 장바구니와 조회 결과는 초기화했습니다.");
      await loadMenus();
    } catch (error) { if ($("settings-dialog").open) feedback("settings-feedback", errorText(error)); else announce(errorText(error), true); }
  });
  app.runtime = { getClient: () => state.client, isPreview, isBusy: () => state.busy || isPreview(), setBusy, announce, feedback, errorText, node, button };
  $("delivery-date").value = D.todaySeoul();
  try { makeClient(); renderCart(); loadMenus().catch(() => {}); }
  catch (error) { announce(errorText(error), true); $("menu-status").textContent = errorText(error); $("menu-status").className = "empty-state error"; }
})(window);
