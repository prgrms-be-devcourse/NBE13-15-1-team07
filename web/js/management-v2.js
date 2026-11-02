(function (root) {
  "use strict";
  const app = root.CoffeeLab, R = app.runtime, D = app.domain;
  const $ = id => document.getElementById(id);
  const won = value => new Intl.NumberFormat("ko-KR").format(value) + "원";
  let editing = null, deleting = null, generation = 0, categorySeq = 0;
  function resetEditor() {
    editing = null; $("category-form").reset(); $("save-category").textContent = "등록"; $("cancel-category-edit").hidden = true;
  }
  function fillOptions(id, categories) {
    const select = $(id), previous = select.value;
    const placeholder = R.node("option", "카테고리를 선택하세요"); placeholder.value = "";
    select.replaceChildren(placeholder);
    for (const item of categories) { const option = R.node("option", item.name); option.value = String(item.id); select.append(option); }
    if (categories.some(item => String(item.id) === previous)) select.value = previous;
  }
  async function run(feedbackId, task) {
    if (R.isBusy()) return;
    const client = R.getClient(), token = generation;
    R.setBusy(true); R.feedback(feedbackId, "");
    try { await task(client, () => token === generation && client === R.getClient()); }
    catch (error) { if (token === generation) R.feedback(feedbackId, R.errorText(error) + (error.uncertain ? "\n처리 결과를 목록이나 DB에서 확인한 뒤 다시 요청해 주세요." : "")); }
    finally { R.setBusy(false); }
  }
  async function refreshCategories() {
    const client = R.getClient(), token = generation, seq = ++categorySeq;
    try {
      const categories = R.isPreview() ? app.previewSnapshot.categories : await client.listCategories();
      if (token !== generation || client !== R.getClient() || seq !== categorySeq) return;
      fillOptions("menu-category", categories); fillOptions("category-filter", categories);
      const list = $("category-list"); list.replaceChildren();
      if (!categories.length) list.append(R.node("p", "아직 등록한 카테고리가 없습니다.", "muted"));
      for (const category of categories) {
        const row = R.node("article", undefined, "category-row");
        row.append(R.node("span", category.name + " (#" + category.id + ")"));
        const actions = R.node("div", undefined, "button-row");
        const edit = R.button("수정", () => run("category-feedback", async (current, valid) => {
          const value = await current.getCategory(category.id); if (!valid()) return;
          editing = value.id; $("category-name").value = value.name; $("save-category").textContent = "수정 저장"; $("cancel-category-edit").hidden = false; $("category-name").focus();
        }), "button outline small");
        edit.setAttribute("aria-label", category.name + " 카테고리 수정");
        const remove = R.button("삭제", () => {
          if (R.isBusy()) return; deleting = category.id;
          $("category-delete-description").textContent = "‘" + category.name + "’ 카테고리를 삭제합니다."; $("category-delete-dialog").showModal();
        }, "button subtle small");
        remove.setAttribute("aria-label", category.name + " 카테고리 삭제");
        actions.append(edit, remove); row.append(actions); list.append(row);
      }
      if (R.isPreview()) renderPreviewManagement();
    } catch (error) {
      if (token === generation && seq === categorySeq) { $("category-list").replaceChildren(); R.feedback("category-feedback", R.errorText(error)); }
      throw error;
    }
  }
  async function refreshAfterSave(text) {
    try { await refreshCategories(); R.feedback("category-feedback", text, true); }
    catch (_) { R.feedback("category-feedback", text + " 목록을 갱신하지 못했으니 새로고침으로 확인해 주세요."); }
    $("category-menu-results").replaceChildren();
  }
  $("reload-categories").addEventListener("click", () => run("category-feedback", () => refreshCategories()));
  $("cancel-category-edit").addEventListener("click", resetEditor);
  $("category-form").addEventListener("submit", event => {
    event.preventDefault(); const input = { name: $("category-name").value }, id = editing;
    run("category-feedback", async (client, valid) => {
      const saved = id === null ? await client.createCategory(input) : await client.updateCategory(id, input);
      if (!valid()) return; resetEditor(); await refreshAfterSave("카테고리를 저장했습니다. (#" + saved.id + ")");
    });
  });
  $("close-category-delete").addEventListener("click", () => { deleting = null; $("category-delete-dialog").close(); });
  $("confirm-category-delete").addEventListener("click", () => {
    if (deleting === null) return;
    const id = deleting;
    run("category-feedback", async (client, valid) => {
      try { await client.deleteCategory(id); }
      finally { $("category-delete-dialog").close(); deleting = null; }
      if (!valid()) return; if (editing === id) resetEditor(); await refreshAfterSave("카테고리를 삭제했습니다.");
    });
  });
  function table(headers, rows) {
    const wrap = R.node("div", undefined, "data-table-wrap"), table = R.node("table", undefined, "data-table"), head = R.node("thead"), tr = R.node("tr");
    for (const text of headers) { const th = R.node("th", text); th.scope = "col"; tr.append(th); }
    head.append(tr); table.append(head); const body = R.node("tbody");
    for (const values of rows) { const row = R.node("tr"); for (const value of values) row.append(R.node("td", value)); body.append(row); }
    table.append(body); wrap.append(table); return wrap;
  }
  function renderCategoryMenus(menus) {
    $("category-menu-results").replaceChildren(menus.length ? table(["메뉴 ID", "메뉴명", "가격", "판매 상태"], menus.map(menu => [menu.id, menu.name, won(menu.price), menu.available ? "판매 중" : "판매 중지"])) : R.node("p", "이 카테고리에 등록한 메뉴가 없습니다.", "muted"));
  }
  $("category-filter-form").addEventListener("submit", event => {
    event.preventDefault(); const id = Number($("category-filter").value);
    run("category-feedback", async (client, valid) => {
      if (!D.id(id)) throw D.error("VALIDATION_ERROR", { categoryId: "카테고리를 선택해 주세요." });
      const menus = await client.listCategoryMenus(id); if (!valid()) return;
      renderCategoryMenus(menus);
    });
  });
  function clearStatistics() { $("order-statistics").replaceChildren(); $("menu-statistics").replaceChildren(); $("customer-statistics").replaceChildren(); R.feedback("statistics-feedback", "주문 상태가 바뀌면 통계를 다시 조회해 주세요."); }
  function renderStatistics(results) {
    $("order-statistics").replaceChildren(); $("menu-statistics").replaceChildren(); $("customer-statistics").replaceChildren();
    const errors = [];
    if (results[0].status === "fulfilled") {
      const values = results[0].value, summary = R.node("div", undefined, "stats-summary");
      for (const [label, value] of [["전체 주문", values.orderCount + "건"], ["접수 주문", values.placedOrderCount + "건"], ["취소 주문", values.cancelledOrderCount + "건"], ["접수 수량", values.totalQuantity + "개"], ["접수 금액", won(values.totalAmount)]]) {
        const box = R.node("div", undefined, "stat-box"); box.append(R.node("span", label), R.node("strong", value)); summary.append(box);
      }
      $("order-statistics").append(summary);
    } else errors.push("주문 통계: " + R.errorText(results[0].reason));
    if (results[1].status === "fulfilled") {
      const rows = results[1].value;
      $("menu-statistics").append(R.node("h3", "메뉴별 통계"));
      $("menu-statistics").append(rows.length ? table(["메뉴명", "접수 단가", "수량", "금액"], rows.map(item => [item.menuName, won(item.unitPrice), item.totalQuantity, won(item.totalAmount)])) : R.node("p", "선택한 접수일의 취소되지 않은 주문이 없습니다.", "muted"));
    } else errors.push("메뉴 통계: " + R.errorText(results[1].reason));
    if (results[2].status === "fulfilled") {
      const rows = results[2].value; $("customer-statistics").append(R.node("h3", "고객별 통계"));
      $("customer-statistics").append(rows.length ? table(["이메일", "전체 주문", "접수", "취소", "접수 수량", "접수 금액"], rows.map(item => [item.email, item.orderCount, item.placedOrderCount, item.cancelledOrderCount, item.totalQuantity, won(item.totalAmount)])) : R.node("p", "선택한 접수일의 주문이 없습니다.", "muted"));
    } else errors.push("고객별 통계: " + R.errorText(results[2].reason));
    R.feedback("statistics-feedback", errors.join("\n") || "접수일 기준으로 주문·메뉴·고객 통계를 조회했습니다.", errors.length === 0);
  }
  function renderPreviewManagement() {
    if (!R.isPreview()) return;
    const sample = app.previewSnapshot;
    $("category-filter").value = String(sample.categories[0].id);
    renderCategoryMenus(sample.menus);
    $("statistics-date").value = sample.date;
    renderStatistics([
      { status: "fulfilled", value: sample.orderStatistics },
      { status: "fulfilled", value: sample.menuStatistics },
      { status: "fulfilled", value: sample.customerStatistics }
    ]);
    R.feedback("category-feedback", "고정된 예시 카테고리·메뉴입니다.");
    R.feedback("statistics-feedback", "고정된 예시 통계입니다. 실제 조회는 API 연결 후 사용할 수 있습니다.");
    R.setBusy(false);
  }
  $("statistics-form").addEventListener("submit", event => {
    event.preventDefault(); const date = $("statistics-date").value;
    run("statistics-feedback", async (client, valid) => {
      const results = await Promise.allSettled([client.getOrderStatistics(date), client.getMenuStatistics(date), client.getCustomerStatistics(date)]);
      if (valid()) renderStatistics(results);
    });
  });
  document.addEventListener("coffeelab:orders-changed", clearStatistics);
  document.addEventListener("coffeelab:menus-changed", () => $("category-menu-results").replaceChildren());
  document.addEventListener("coffeelab:connection-changed", () => {
    generation++; resetEditor(); $("category-list").replaceChildren(); $("category-menu-results").replaceChildren(); clearStatistics();
    fillOptions("menu-category", []); fillOptions("category-filter", []);
    $("statistics-date").value = R.isPreview() ? app.previewSnapshot.date : D.todaySeoul();
    refreshCategories().catch(() => {});
  });
  $("statistics-date").value = D.todaySeoul();
  refreshCategories().catch(() => {});
})(window);
