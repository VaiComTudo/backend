import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 }, // Ramp up to 10 users
    { duration: '1m', target: 10 },  // Stay at 10 users
    { duration: '30s', target: 20 }, // Ramp up to 20 users
    { duration: '1m', target: 20 },  // Stay at 20 users
    { duration: '30s', target: 0 },  // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.01'],   // Error rate must be below 1%
    errors: ['rate<0.1'],             // Custom error rate must be below 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Helper function to generate random data
function randomEmail() {
  return `user${Math.floor(Math.random() * 100000)}@test.com`;
}

function randomString(length = 8) {
  const chars = 'abcdefghijklmnopqrstuvwxyz';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars[Math.floor(Math.random() * chars.length)];
  }
  return result;
}

// Authentication flow
function registerUser(email, password, role) {
  const payload = JSON.stringify({
    name: 'Test User',
    email: email,
    password: password,
    birthdate: '1990-01-01',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/auth/register?role=${role}`,
    payload,
    params
  );

  const success = check(res, {
    'registration status is 200': (r) => r.status === 200,
    'registration returns token': (r) => r.json('token') !== undefined,
  });

  errorRate.add(!success);

  return res.json('token');
}

function loginUser(email, password) {
  const payload = JSON.stringify({
    email: email,
    password: password,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    payload,
    params
  );

  const success = check(res, {
    'login status is 200': (r) => r.status === 200,
    'login returns token': (r) => r.json('token') !== undefined,
  });

  errorRate.add(!success);

  return res.json('token');
}

// Renter endpoints
function searchListings(token) {
  const params = {
    headers: token ? {
      'Authorization': `Bearer ${token}`,
    } : {},
  };

  const res = http.get(
    `${BASE_URL}/api/v1/renters/listings?page=0&size=20&sortBy=title&sortDirection=asc`,
    params
  );

  const success = check(res, {
    'search listings status is 200': (r) => r.status === 200,
    'search listings returns content': (r) => r.json('content') !== undefined,
  });

  errorRate.add(!success);

  return res.json('content');
}

function getListingById(listingId, token) {
  const params = {
    headers: token ? {
      'Authorization': `Bearer ${token}`,
    } : {},
  };

  const res = http.get(
    `${BASE_URL}/api/v1/renters/listings/${listingId}`,
    params
  );

  const success = check(res, {
    'get listing status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
}

// Owner endpoints
function createListing(token) {
  const payload = JSON.stringify({
    title: `Test Listing ${randomString()}`,
    description: 'A test listing for k6 load testing',
    pickUpLocation: 'Aveiro',
    dropOffLocation: 'Aveiro',
    price: 50.00,
    state: 'AVAILABLE',
    vehicle: {
      type: 'SUV',
      condition: 'EXCELLENT',
    },
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/owners/listings`,
    payload,
    params
  );

  const success = check(res, {
    'create listing status is 201': (r) => r.status === 201,
    'create listing returns id': (r) => r.json('id') !== undefined,
  });

  errorRate.add(!success);

  return res.json('id');
}

function getOwnerListings(token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  };

  const res = http.get(
    `${BASE_URL}/api/v1/owners/listings?page=0&size=10&sortBy=title&sortDirection=asc`,
    params
  );

  const success = check(res, {
    'get owner listings status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
}

// Booking endpoints
function createBooking(token, listingId) {
  const today = new Date();
  const pickupDate = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 days from now
  const dropoffDate = new Date(today.getTime() + 14 * 24 * 60 * 60 * 1000); // 14 days from now

  const payload = JSON.stringify({
    listingId: listingId,
    pickupDateTime: pickupDate.toISOString(),
    dropoffDateTime: dropoffDate.toISOString(),
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };

  const res = http.post(
    `${BASE_URL}/api/v1/bookings`,
    payload,
    params
  );

  const success = check(res, {
    'create booking status is 201': (r) => r.status === 201,
  });

  errorRate.add(!success);

  return res.json('id');
}

function getRenterBookings(token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  };

  const res = http.get(
    `${BASE_URL}/api/v1/bookings/renter`,
    params
  );

  const success = check(res, {
    'get renter bookings status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
}

function getOwnerBookings(token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  };

  const res = http.get(
    `${BASE_URL}/api/v1/bookings/owner`,
    params
  );

  const success = check(res, {
    'get owner bookings status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
}

// Main test scenario
export default function () {
  const scenario = Math.random();

  if (scenario < 0.6) {
    // 60% - Renter browsing listings
    const renterEmail = randomEmail();
    const renterPassword = 'Password123!';
    
    // Some users browse without logging in
    if (Math.random() < 0.5) {
      const listings = searchListings(null);
      sleep(1);
      
      if (listings && listings.length > 0) {
        const randomListing = listings[Math.floor(Math.random() * listings.length)];
        getListingById(randomListing.id, null);
        sleep(1);
      }
    } else {
      // Registered renter browsing
      const token = registerUser(renterEmail, renterPassword, 'NORMAL_USER');
      sleep(1);
      
      const listings = searchListings(token);
      sleep(1);
      
      if (listings && listings.length > 0) {
        const randomListing = listings[Math.floor(Math.random() * listings.length)];
        getListingById(randomListing.id, token);
        sleep(1);
      }
    }
  } else if (scenario < 0.8) {
    // 20% - Owner managing listings
    const ownerEmail = randomEmail();
    const ownerPassword = 'Password123!';
    
    const token = registerUser(ownerEmail, ownerPassword, 'NORMAL_USER');
    sleep(1);
    
    // Create a listing
    createListing(token);
    sleep(1);
    
    // Check own listings
    getOwnerListings(token);
    sleep(1);
  } else {
    // 20% - Complete booking flow (owner creates listing, renter books it)
    const ownerEmail = randomEmail();
    const ownerPassword = 'Password123!';
    const renterEmail = randomEmail();
    const renterPassword = 'Password123!';
    
    // Owner creates a listing
    const ownerToken = registerUser(ownerEmail, ownerPassword, 'NORMAL_USER');
    sleep(1);
    
    const listingId = createListing(ownerToken);
    sleep(1);
    
    if (listingId) {
      // Different user (renter) books the listing
      const renterToken = registerUser(renterEmail, renterPassword, 'NORMAL_USER');
      sleep(1);
      
      // Create a booking
      createBooking(renterToken, listingId);
      sleep(1);
      
      // Check renter bookings
      getRenterBookings(renterToken);
      sleep(1);
      
      // Check owner bookings
      getOwnerBookings(ownerToken);
      sleep(1);
    }
  }

  sleep(Math.random() * 3 + 1); // Random sleep between 1-4 seconds
}
