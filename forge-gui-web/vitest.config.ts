import { defineConfig } from 'vitest/config';

// The browser client's own tests, which run on Node because what they test never touches the page
export default defineConfig({
  test: {
    include: ['src/test/ts/**/*.test.ts'],
    setupFiles: ['src/test/ts/text-setup.ts'],
  },
});
