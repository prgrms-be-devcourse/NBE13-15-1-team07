/* 명세서에 정의한 24개 API 호출 메서드를 확인합니다. */
(function (root) {
  "use strict";
  const app = root.CoffeeLab, endpoints = app.contract.endpoints;
  if (endpoints.length !== 24 || new Set(endpoints.map(v => v.method + " " + v.path.split("?")[0])).size !== 24) throw new Error("24 API 설정이 중복되거나 누락되었습니다.");
  for (const entry of endpoints) {
    if (typeof app.ApiClient.prototype[entry.clientMethod] !== "function") throw new Error("클라이언트 메서드 누락: " + entry.clientMethod);
  }
  app.requiredMethods = Object.freeze(endpoints.map(v => v.clientMethod));
})(typeof window !== "undefined" ? window : globalThis);
