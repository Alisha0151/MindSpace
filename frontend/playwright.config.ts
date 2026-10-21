import { defineConfig } from '@playwright/test';

// E2E needs the whole stack running (docker compose up) and Playwright browsers installed.
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  use: { baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8081', trace: 'retain-on-failure' },
});
