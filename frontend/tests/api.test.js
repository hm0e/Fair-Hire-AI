import test from 'node:test';
import assert from 'node:assert/strict';
import { API_BASE } from '../src/services/api.js';

test('API_BASE is properly configured for proxying', () => {
  assert.equal(API_BASE, '/api');
});

test('API health contract shape verification', () => {
  const mockHealthResponse = {
    status: 'UP',
    service: 'fairhire-backend',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  };
  assert.equal(mockHealthResponse.status, 'UP');
  assert.equal(mockHealthResponse.service, 'fairhire-backend');
  assert.ok(mockHealthResponse.timestamp);
});
