/* 명세서 v2 · 필수 24 API 설정. 원본 18 API JSON과는 별도 버전입니다. */
window.CoffeeLab = window.CoffeeLab || {};
window.CoffeeLab.contract = {
  "version": "3.0.0",
  "projectName": "Grids & Circle",
  "apiBaseUrl": "http://127.0.0.1:8080",
  "frontendOrigin": "http://127.0.0.1:8000",
  "timeoutMs": 15000,
  "timeZone": "Asia/Seoul",
  "limits": {
    "nameMax": 100,
    "descriptionMax": 300,
    "priceMin": 1,
    "priceMax": 1000000,
    "emailMax": 100,
    "addressMax": 200,
    "postcodeLength": 5,
    "maxDistinctItems": 10,
    "quantityMin": 1,
    "quantityMax": 20,
    "categoryNameMax": 30,
    "memoMax": 200,
    "keywordMax": 100
  },
  "routes": {
    "menus": "/api/v1/menus",
    "menu": "/api/v1/menus/{menuId}",
    "orders": "/api/v1/orders",
    "order": "/api/v1/orders/{orderId}",
    "deliveries": "/api/v1/delivery-batches",
    "categories": "/api/v1/categories",
    "category": "/api/v1/categories/{categoryId}",
    "categoryMenus": "/api/v1/categories/{categoryId}/menus",
    "cancelOrder": "/api/v1/orders/{orderId}/cancel",
    "orderStats": "/api/v1/statistics/orders",
    "menuStats": "/api/v1/statistics/menus",
    "availableMenus": "/api/v1/menus/available",
    "menuSearch": "/api/v1/menus/search",
    "menuAvailability": "/api/v1/menus/{menuId}/availability",
    "orderMemo": "/api/v1/orders/{orderId}/memo",
    "reorder": "/api/v1/orders/{orderId}/reorder",
    "customerStats": "/api/v1/statistics/customers"
  },
  "endpoints": [
    {
      "method": "GET",
      "path": "/api/v1/categories",
      "success": 200,
      "response": "Category[]",
      "clientMethod": "listCategories"
    },
    {
      "method": "GET",
      "path": "/api/v1/categories/{categoryId}",
      "success": 200,
      "response": "Category",
      "clientMethod": "getCategory"
    },
    {
      "method": "POST",
      "path": "/api/v1/categories",
      "success": 201,
      "request": "CategoryInput",
      "response": "Category",
      "clientMethod": "createCategory"
    },
    {
      "method": "PUT",
      "path": "/api/v1/categories/{categoryId}",
      "success": 200,
      "request": "CategoryInput",
      "response": "Category",
      "clientMethod": "updateCategory"
    },
    {
      "method": "DELETE",
      "path": "/api/v1/categories/{categoryId}",
      "success": 204,
      "response": "empty",
      "clientMethod": "deleteCategory"
    },
    {
      "method": "GET",
      "path": "/api/v1/menus",
      "success": 200,
      "response": "Menu[]",
      "clientMethod": "listMenus"
    },
    {
      "method": "GET",
      "path": "/api/v1/menus/{menuId}",
      "success": 200,
      "response": "Menu",
      "clientMethod": "getMenu"
    },
    {
      "method": "POST",
      "path": "/api/v1/menus",
      "success": 201,
      "request": "MenuInput",
      "response": "Menu",
      "clientMethod": "createMenu"
    },
    {
      "method": "PUT",
      "path": "/api/v1/menus/{menuId}",
      "success": 200,
      "request": "MenuInput",
      "response": "Menu",
      "clientMethod": "updateMenu"
    },
    {
      "method": "DELETE",
      "path": "/api/v1/menus/{menuId}",
      "success": 204,
      "response": "empty",
      "clientMethod": "deleteMenu"
    },
    {
      "method": "POST",
      "path": "/api/v1/orders",
      "success": 201,
      "request": "OrderInput",
      "response": "Order",
      "clientMethod": "createOrder"
    },
    {
      "method": "GET",
      "path": "/api/v1/orders?email={email}",
      "success": 200,
      "response": "OrderSummary[]",
      "clientMethod": "listOrders"
    },
    {
      "method": "GET",
      "path": "/api/v1/orders/{orderId}",
      "success": 200,
      "response": "Order",
      "clientMethod": "getOrder"
    },
    {
      "method": "GET",
      "path": "/api/v1/delivery-batches?shippingDate={shippingDate}",
      "success": 200,
      "response": "DeliveryBatch[]",
      "clientMethod": "listDeliveries"
    },
    {
      "method": "GET",
      "path": "/api/v1/categories/{categoryId}/menus",
      "success": 200,
      "response": "Menu[]",
      "clientMethod": "listCategoryMenus"
    },
    {
      "method": "POST",
      "path": "/api/v1/orders/{orderId}/cancel",
      "success": 200,
      "response": "Order",
      "clientMethod": "cancelOrder"
    },
    {
      "method": "GET",
      "path": "/api/v1/statistics/orders?date={date}",
      "success": 200,
      "response": "OrderStatistics",
      "clientMethod": "getOrderStatistics"
    },
    {
      "method": "GET",
      "path": "/api/v1/statistics/menus?date={date}",
      "success": 200,
      "response": "MenuStatistics[]",
      "clientMethod": "getMenuStatistics"
    },
    {
      "method": "GET",
      "path": "/api/v1/menus/available",
      "success": 200,
      "response": "Menu[]",
      "clientMethod": "listAvailableMenus"
    },
    {
      "method": "GET",
      "path": "/api/v1/menus/search?keyword={keyword}",
      "success": 200,
      "response": "Menu[]",
      "clientMethod": "searchMenus"
    },
    {
      "method": "PUT",
      "path": "/api/v1/menus/{menuId}/availability",
      "success": 200,
      "request": "AvailabilityInput",
      "response": "Menu",
      "clientMethod": "updateMenuAvailability"
    },
    {
      "method": "PUT",
      "path": "/api/v1/orders/{orderId}/memo",
      "success": 200,
      "request": "MemoInput",
      "response": "Order",
      "clientMethod": "updateOrderMemo"
    },
    {
      "method": "POST",
      "path": "/api/v1/orders/{orderId}/reorder",
      "success": 201,
      "response": "Order",
      "clientMethod": "reorder"
    },
    {
      "method": "GET",
      "path": "/api/v1/statistics/customers?date={date}",
      "success": 200,
      "response": "CustomerStatistics[]",
      "clientMethod": "getCustomerStatistics"
    }
  ],
  "errors": {
    "VALIDATION_ERROR": {
      "status": 400,
      "message": "입력값을 확인해 주세요."
    },
    "MENU_NOT_FOUND": {
      "status": 404,
      "message": "메뉴를 찾을 수 없습니다."
    },
    "ORDER_NOT_FOUND": {
      "status": 404,
      "message": "주문을 찾을 수 없습니다."
    },
    "MENU_IN_USE": {
      "status": 409,
      "message": "주문에 사용된 메뉴는 삭제할 수 없습니다."
    },
    "ADDRESS_MISMATCH": {
      "status": 409,
      "message": "같은 이메일과 배송일의 기존 주문과 배송지가 다릅니다."
    },
    "CATEGORY_NOT_FOUND": {
      "status": 404,
      "message": "카테고리를 찾을 수 없습니다."
    },
    "CATEGORY_NAME_DUPLICATED": {
      "status": 409,
      "message": "같은 이름의 카테고리가 이미 있습니다."
    },
    "CATEGORY_IN_USE": {
      "status": 409,
      "message": "메뉴가 등록된 카테고리는 삭제할 수 없습니다."
    },
    "ORDER_NOT_CANCELLABLE": {
      "status": 409,
      "message": "배송 대상일 14시 전의 접수 상태 주문만 취소할 수 있습니다."
    },
    "MENU_UNAVAILABLE": {
      "status": 409,
      "message": "판매 중지된 메뉴가 포함되어 있습니다."
    }
  },
  "seedMenus": [
    {
      "id": 1,
      "name": "Columbia Nariñó",
      "price": 5000,
      "description": "콜롬비아 나리뇨 원두를 담은 커피빈 패키지입니다.",
      "categoryId": 1,
      "available": true
    },
    {
      "id": 2,
      "name": "Brazil Serra Do Caparaó",
      "price": 7000,
      "description": "브라질 세하 두 카파라오 원두를 담은 커피빈 패키지입니다.",
      "categoryId": 1,
      "available": true
    },
    {
      "id": 3,
      "name": "Columbia Quindío (White Wine Extended Fermentation)",
      "price": 9000,
      "description": "콜롬비아 킨디오 원두를 담은 커피빈 패키지입니다.",
      "categoryId": 1,
      "available": true
    },
    {
      "id": 4,
      "name": "Ethiopia Sidamo",
      "price": 11000,
      "description": "에티오피아 시다모 원두를 담은 커피빈 패키지입니다.",
      "categoryId": 1,
      "available": true
    }
  ],
  "seedCategories": [
    {
      "id": 1,
      "name": "싱글오리진"
    },
    {
      "id": 2,
      "name": "블렌드"
    }
  ],
  "apiCount": 24
};
