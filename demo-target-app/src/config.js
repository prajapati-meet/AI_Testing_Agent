export const DEMO_CONFIG = {
  DEMO_EMAIL: 'test@example.com',
  DEMO_PASSWORD: 'password123',
  BUG_MODE: String(import.meta.env.BUG_MODE || import.meta.env.VITE_BUG_MODE || 'false').toLowerCase() === 'true'
};
