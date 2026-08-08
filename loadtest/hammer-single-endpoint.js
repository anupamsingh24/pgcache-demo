import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const MAX_PRODUCT_ID = parseInt(__ENV.MAX_PRODUCT_ID || '50000', 10);

export const options = {
  scenarios: {
    hammer: {
      executor: 'constant-vus',
      vus: parseInt(__ENV.VUS || '50', 10),
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const id = Math.floor(Math.random() * MAX_PRODUCT_ID) + 1;
  const res = http.get(`${BASE_URL}/api/products/${id}`);
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.05);
}
