import * as dotenv from 'dotenv';
import * as path from 'path';

// Force load .env.test overriding anything else
const result = dotenv.config({ path: path.resolve(process.cwd(), '.env.test'), override: true });

if (result.error) {
  throw new Error('CRITICAL: Failed to load .env.test file. Refusing to run tests.');
}

// Strict safety check to prevent running against dev/prod
if (!process.env.DATABASE_URL || !process.env.DATABASE_URL.includes('foodplatform_test')) {
  throw new Error('CRITICAL: Test DATABASE_URL is missing or does not contain "foodplatform_test". Refusing to run tests to prevent data loss.');
}
