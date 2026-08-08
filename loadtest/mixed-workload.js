import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const MAX_PRODUCT_ID = 50000;
const MAX_ORDER_ID = 200000;
const CATEGORY_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const SEARCH_TERMS = ['Product 1', 'Product 2', 'Product 3', 'Product 12'];

export const options = {
  scenarios: {
    mixed: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 30 },
        { duration: '2m', target: 30 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<800'],
  },
};

function randomInt(max) {
  return Math.floor(Math.random() * max) + 1;
}

function get(path, tagName) {
  const res = http.get(`${BASE_URL}${path}`, { tags: { name: tagName } });
  check(res, { [`${tagName} status ok`]: (r) => r.status < 500 });
  return res;
}

export default function () {
  const roll = Math.random();

  if (roll < 0.40) {
    // single-table SELECT
    get(`/api/products/${randomInt(MAX_PRODUCT_ID)}`, 'product_lookup');
  } else if (roll < 0.60) {
    // WHERE + ORDER BY + LIMIT/OFFSET
    const category = CATEGORY_IDS[randomInt(CATEGORY_IDS.length) - 1];
    const sort = Math.random() < 0.5 ? 'price' : 'name';
    const page = randomInt(5) - 1;
    get(`/api/products?category=${category}&sort=${sort}&page=${page}&size=20`, 'product_list');
  } else if (roll < 0.75) {
    // ILIKE pattern match
    const term = SEARCH_TERMS[randomInt(SEARCH_TERMS.length) - 1];
    get(`/api/products/search?q=${encodeURIComponent(term)}`, 'product_search');
  } else if (roll < 0.90) {
    // INNER JOIN
    get(`/api/orders/${randomInt(MAX_ORDER_ID)}`, 'order_lookup');
  } else if (roll < 0.96) {
    // multi-way JOIN + GROUP BY + SUM
    get('/api/analytics/revenue-by-category', 'analytics_revenue');
  } else if (roll < 0.99) {
    // window function
    get('/api/analytics/top-products?limit=10', 'analytics_top_products');
  } else {
    // write — triggers CDC invalidation on the cached path
    const id = randomInt(MAX_PRODUCT_ID);
    const newPrice = (Math.random() * 190 + 10).toFixed(2);
    const res = http.patch(
      `${BASE_URL}/api/products/${id}/price`,
      JSON.stringify({ price: newPrice }),
      { headers: { 'Content-Type': 'application/json' }, tags: { name: 'price_update' } }
    );
    check(res, { 'price_update status ok': (r) => r.status < 500 });
  }

  sleep(Math.random() * 0.3);
}
