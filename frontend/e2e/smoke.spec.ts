import { expect, test } from '@playwright/test';

// Requires the full stack (docker compose up) and `npx playwright install chromium`.
const email = () => `e2e-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.test`;
const PASSWORD = 'synthetic-e2e-passphrase-42';

async function register(page: import('@playwright/test').Page) {
  await page.goto('/register');
  await page.getByLabel('Name', { exact: true }).fill('E2E Tester');
  await page.getByLabel('E-mail').fill(email());
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByLabel('Confirm password').fill(PASSWORD);
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: /create account|register|sign up/i }).click();
  await expect(page).toHaveURL(/\/app/);
}

test('anonymous visitors are redirected away from the app', async ({ page }) => {
  await page.goto('/app/journal');
  await expect(page).toHaveURL(/\/login/);
});

test('register, check in, write a journal entry, complete a habit, then sign out', async ({ page }) => {
  await register(page);

  await page.goto('/app/check-in');
  await page.getByRole('button', { name: 'Save check-in' }).click();
  await expect(page.getByText('Check-in saved')).toBeVisible();

  await page.goto('/app/journal/new');
  await page.getByLabel('Entry').fill('A synthetic entry written by the end-to-end test.');
  await page.getByRole('button', { name: 'Save entry' }).click();
  await expect(page.getByText('Entry saved')).toBeVisible();
  await page.goto('/app/journal');
  await page.getByLabel('Search entries').fill('synthetic');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page.getByText('A synthetic entry')).toBeVisible();

  await page.goto('/app/habits');
  await page.getByRole('button', { name: 'Add habit' }).click();
  await page.getByLabel('Name').fill('Stretch');
  await page.getByRole('button', { name: 'Save' }).click();
  await page.getByRole('button', { name: /Mark done today/ }).click();
  await expect(page.getByRole('button', { name: /Done today/ })).toBeVisible();

  await page.getByRole('button', { name: 'Sign out' }).first().click();
  await page.goto('/app');
  await expect(page).toHaveURL(/\/login/);
});

test('the help page lists verified crisis resources', async ({ page }) => {
  await register(page);
  await page.goto('/app/help');
  await expect(page.getByText('988').first()).toBeVisible();
  await expect(page.getByText('14416').first()).toBeVisible();
});
