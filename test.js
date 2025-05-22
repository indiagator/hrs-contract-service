import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 500 }, // Ramp-up to 500 RPS over 1 minute
        { duration: '1m', target: 500 }, // Sustain 500 RPS for 5 minutes
        { duration: '30s', target: 0 },   // Ramp-down to 0 RPS over 1 minute
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests should complete below 500ms
    },
};

const BASE_URL = 'http://localhost:8072/contract-service/api/v1/float/project';
const AUTH_TOKEN = '859760884'; // Replace with a valid token if needed

export default function () {
    const payload = JSON.stringify({
        name: 'Test Project',
        location: 'Test Location',
        status: 'DRAFT',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': AUTH_TOKEN,
        },
    };

    const res = http.post(BASE_URL, payload, params);

    check(res, {
        'is status 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });
}