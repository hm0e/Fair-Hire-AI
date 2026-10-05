/**
 * FairHire AI - Core API Client Service
 * Communicates with the Spring Boot Backend (Port 8088 / /api/v1)
 */

export const API_BASE = '/api';

/**
 * Fetch Spring Boot Backend Health Status
 */
export async function fetchHealth() {
  const response = await fetch(`${API_BASE}/v1/health`);
  if (!response.ok) {
    throw new Error(`Health check failed with status: ${response.status}`);
  }
  return response.json();
}

/**
 * Fetch Comprehensive System Health (Backend + PostgreSQL + AI Service)
 */
export async function fetchSystemHealth() {
  const response = await fetch(`${API_BASE}/v1/health/system`);
  if (!response.ok) {
    throw new Error(`System health check failed with status: ${response.status}`);
  }
  return response.json();
}
