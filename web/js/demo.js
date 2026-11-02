/* 고정 예시 화면에 표시할 값입니다. */
(function (root) {
  "use strict";
  const app = root.CoffeeLab;
  app.previewSnapshot = Object.freeze({
    categories: app.contract.seedCategories,
    menus: app.contract.seedMenus,
    "date": "2026-11-03",
    "orderSummaries": [
      {
        "id": 101,
        "createdAt": "2026-11-03T13:00:00+09:00",
        "shippingDate": "2026-11-03",
        "totalAmount": 17000,
        "status": "PLACED"
      }
    ],
    "order": {
      "id": 101,
      "email": "buyer@example.com",
      "address": "서울시 실습로 10 101호",
      "postcode": "01234",
      "createdAt": "2026-11-03T13:00:00+09:00",
      "shippingDate": "2026-11-03",
      "totalAmount": 17000,
      "items": [
        {
          "menuId": 1,
          "menuName": "Columbia Nariñó",
          "unitPrice": 5000,
          "quantity": 2,
          "lineAmount": 10000
        },
        {
          "menuId": 2,
          "menuName": "Brazil Serra Do Caparaó",
          "unitPrice": 7000,
          "quantity": 1,
          "lineAmount": 7000
        }
      ],
      "status": "PLACED",
      "cancelledAt": null,
      "memo": ""
    },
    "deliveryBatches": [
      {
        "shippingDate": "2026-11-03",
        "email": "buyer@example.com",
        "address": "서울시 실습로 10 101호",
        "postcode": "01234",
        "orderIds": [
          101
        ],
        "orderCount": 1,
        "totalQuantity": 3,
        "totalAmount": 17000,
        "items": [
          {
            "menuId": 1,
            "menuName": "Columbia Nariñó",
            "unitPrice": 5000,
            "quantity": 2,
            "lineAmount": 10000
          },
          {
            "menuId": 2,
            "menuName": "Brazil Serra Do Caparaó",
            "unitPrice": 7000,
            "quantity": 1,
            "lineAmount": 7000
          }
        ]
      }
    ],
    "orderStatistics": {
      "date": "2026-11-03",
      "orderCount": 1,
      "placedOrderCount": 1,
      "cancelledOrderCount": 0,
      "totalQuantity": 3,
      "totalAmount": 17000
    },
    "menuStatistics": [
      {
        "menuId": 1,
        "menuName": "Columbia Nariñó",
        "unitPrice": 5000,
        "totalQuantity": 2,
        "totalAmount": 10000
      },
      {
        "menuId": 2,
        "menuName": "Brazil Serra Do Caparaó",
        "unitPrice": 7000,
        "totalQuantity": 1,
        "totalAmount": 7000
      }
    ],
    "customerStatistics": [
      {
        "date": "2026-11-03",
        "email": "buyer@example.com",
        "orderCount": 1,
        "placedOrderCount": 1,
        "cancelledOrderCount": 0,
        "totalQuantity": 3,
        "totalAmount": 17000
      }
    ]
  });
})(typeof window !== "undefined" ? window : globalThis);
