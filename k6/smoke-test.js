import http from 'k6/http';
import { check, sleep } from 'k6';

// Smoke test - verify basic functionality with minimal load
export const options = {
  vus: 1, // 1 virtual user
  duration: '1m', // Run for 1 minute
  thresholds: {
    http_req_duration: ['p(99)<1500'], // 99% of requests must complete below 1500ms
    http_req_failed: ['rate<0.01'],    // Error rate must be below 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Test 1: Register a new user
  const registerPayload = JSON.stringify({
    name: 'Smoke Test User',
    email: `smoketest${Date.now()}@test.com`,
    password: 'SmokeTest123!',
    birthdate: '1990-01-01',
  });

  const registerRes = http.post(
    `${BASE_URL}/api/v1/auth/register?role=NORMAL_USER`,
    registerPayload,
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );

  check(registerRes, {
    'registration successful': (r) => r.status === 200,
    'token received': (r) => r.json('token') !== undefined,
  });

  const token = registerRes.json('token');
  sleep(1);

  // Test 2: Search listings (public endpoint)
  const searchRes = http.get(
    `${BASE_URL}/api/v1/renters/listings?page=0&size=10`,
    {
      headers: token ? {
        'Authorization': `Bearer ${token}`,
      } : {},
    }
  );

  check(searchRes, {
    'search listings successful': (r) => r.status === 200,
    'listings returned': (r) => r.json('content') !== undefined,
  });
  sleep(1);

  // Test 3: Create a listing (authenticated endpoint)
  const listingPayload = JSON.stringify({
    title: `Smoke Test Listing ${Date.now()}`,
    description: 'A smoke test listing',
    pickUpLocation: 'Aveiro',
    dropOffLocation: 'Aveiro',
    price: 45.00,
    state: 'AVAILABLE',
    vehicle: {
      type: 'Sedan',
      condition: 'EXCELLENT',
    },
  });

  const createListingRes = http.post(
    `${BASE_URL}/api/v1/owners/listings`,
    listingPayload,
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
    }
  );

  check(createListingRes, {
    'listing created': (r) => r.status === 201,
    'listing id returned': (r) => r.json('id') !== undefined,
  });
  sleep(1);

  // Test 4: Get owner listings
  const ownerListingsRes = http.get(
    `${BASE_URL}/api/v1/owners/listings?page=0&size=10`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    }
  );

  check(ownerListingsRes, {
    'owner listings retrieved': (r) => r.status === 200,
  });
  sleep(1);

  // Test 5: Get renter bookings
  const renterBookingsRes = http.get(
    `${BASE_URL}/api/v1/bookings/renter`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    }
  );

  check(renterBookingsRes, {
    'renter bookings retrieved': (r) => r.status === 200,
  });
  sleep(1);
}
