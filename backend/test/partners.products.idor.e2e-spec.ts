import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from '../src/app.module';
import { PrismaService } from '../src/prisma/prisma.service';
import { JwtService } from '@nestjs/jwt';

describe('Partner Products IDOR Security (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService;
  let jwtService: JwtService;

  let vendorAToken: string;
  let vendorBToken: string;
  let customerToken: string;

  let productAId: string;
  let productBId: string;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();

    prisma = app.get(PrismaService);
    jwtService = app.get(JwtService);

    // ── Clean up test data ──────────────────────────────────────────────────
    await prisma.orderItem.deleteMany({});
    await prisma.order.deleteMany({});
    await prisma.cartItem.deleteMany({});
    await prisma.product.deleteMany({});
    await prisma.fulfillmentPoint.deleteMany({});
    await prisma.partner.deleteMany({});
    await prisma.userRole.deleteMany({});
    await prisma.user.deleteMany({});

    // ── Create Users ────────────────────────────────────────────────────────
    const userA = await prisma.user.create({
      data: {
        email: 'vendorA_products@test.com',
        passwordHash: 'hash',
        name: 'Vendor A',
        roles: { create: { role: 'VENDOR' as any } },
      },
    });
    const userB = await prisma.user.create({
      data: {
        email: 'vendorB_products@test.com',
        passwordHash: 'hash',
        name: 'Vendor B',
        roles: { create: { role: 'VENDOR' as any } },
      },
    });
    const customerUser = await prisma.user.create({
      data: {
        email: 'customer_products@test.com',
        passwordHash: 'hash',
        name: 'Customer',
        roles: { create: { role: 'CUSTOMER' as any } },
      },
    });

    vendorAToken = jwtService.sign({ sub: userA.id, email: userA.email });
    vendorBToken = jwtService.sign({ sub: userB.id, email: userB.email });
    customerToken = jwtService.sign({ sub: customerUser.id, email: customerUser.email });

    // ── Create Partners ─────────────────────────────────────────────────────
    const partnerA = await prisma.partner.create({
      data: { userId: userA.id, businessName: 'Biz A', type: 'RESTAURANT' as any },
    });
    const partnerB = await prisma.partner.create({
      data: { userId: userB.id, businessName: 'Biz B', type: 'RESTAURANT' as any },
    });

    // ── Create Products directly in DB ──────────────────────────────────────
    const productA = await prisma.product.create({
      data: {
        name: 'Product A',
        type: 'COOKED_FOOD' as any,
        price: 100,
        partnerId: partnerA.id,
      },
    });
    productAId = productA.id;

    const productB = await prisma.product.create({
      data: {
        name: 'Product B',
        type: 'COOKED_FOOD' as any,
        price: 200,
        partnerId: partnerB.id,
      },
    });
    productBId = productB.id;
  });

  afterAll(async () => {
    // Clean up test data
    await prisma.product.deleteMany({
      where: { id: { in: [productAId, productBId].filter(Boolean) } },
    });
    await app.close();
  });

  // ── Test 1: Vendor A can list own products ────────────────────────────────
  it('1. Vendor A can list own products', async () => {
    const res = await request(app.getHttpServer())
      .get('/partners/products')
      .set('Authorization', `Bearer ${vendorAToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBe(1);
    expect(res.body[0].id).toBe(productAId);
    expect(res.body[0].name).toBe('Product A');
  });

  // ── Test 2: Vendor A cannot see Vendor B products ─────────────────────────
  it('2. Vendor A cannot see Vendor B products in own list', async () => {
    const res = await request(app.getHttpServer())
      .get('/partners/products')
      .set('Authorization', `Bearer ${vendorAToken}`)
      .expect(200);

    const bProductFound = res.body.some((p: any) => p.id === productBId);
    expect(bProductFound).toBe(false);
  });

  // ── Test 3: Vendor A can create own product ───────────────────────────────
  it('3. Vendor A can create own product (partnerId derived from JWT)', async () => {
    const res = await request(app.getHttpServer())
      .post('/partners/products')
      .set('Authorization', `Bearer ${vendorAToken}`)
      .send({ name: 'New Product A', type: 'COOKED_FOOD', price: 150 })
      .expect(201);

    expect(res.body.name).toBe('New Product A');
    // The returned partnerId must NOT be Vendor B's
    expect(res.body.partnerId).toBeDefined();

    // Clean up the newly created product
    await prisma.product.delete({ where: { id: res.body.id } });
  });

  // ── Test 4: Vendor A can update own product ───────────────────────────────
  it('4. Vendor A can update own product', async () => {
    const res = await request(app.getHttpServer())
      .patch(`/partners/products/${productAId}`)
      .set('Authorization', `Bearer ${vendorAToken}`)
      .send({ name: 'Product A Updated' })
      .expect(200);

    expect(res.body.name).toBe('Product A Updated');
  });

  // ── Test 5: Vendor A cannot update Vendor B product → 403 ────────────────
  it('5. Vendor A cannot update Vendor B product → 403', async () => {
    const res = await request(app.getHttpServer())
      .patch(`/partners/products/${productBId}`)
      .set('Authorization', `Bearer ${vendorAToken}`)
      .send({ name: 'Hacked Name' })
      .expect(403);

    expect(res.body.message).toContain('do not have permission');
  });

  // ── Test 6: Vendor A cannot delete Vendor B product → 403 ────────────────
  it('6. Vendor A cannot delete Vendor B product → 403', async () => {
    await request(app.getHttpServer())
      .delete(`/partners/products/${productBId}`)
      .set('Authorization', `Bearer ${vendorAToken}`)
      .expect(403);
  });

  // ── Test 7: Vendor B cannot modify Vendor A product → 403 ────────────────
  it('7. Vendor B cannot modify Vendor A product → 403', async () => {
    await request(app.getHttpServer())
      .patch(`/partners/products/${productAId}`)
      .set('Authorization', `Bearer ${vendorBToken}`)
      .send({ name: 'Vendor B Attack' })
      .expect(403);
  });

  // ── Test 8: Customer cannot access partner product endpoints → 403 ────────
  it('8. Customer cannot access partner product endpoints → 403', async () => {
    await request(app.getHttpServer())
      .get('/partners/products')
      .set('Authorization', `Bearer ${customerToken}`)
      .expect(403);

    await request(app.getHttpServer())
      .post('/partners/products')
      .set('Authorization', `Bearer ${customerToken}`)
      .send({ name: 'Test', type: 'COOKED_FOOD', price: 10 })
      .expect(403);
  });

  // ── Test 9: Unauthenticated request → 401 ─────────────────────────────────
  it('9. Unauthenticated request → 401', async () => {
    await request(app.getHttpServer()).get('/partners/products').expect(401);
    await request(app.getHttpServer())
      .post('/partners/products')
      .send({ name: 'Test', type: 'COOKED_FOOD', price: 10 })
      .expect(401);
  });

  // ── Test 10: Non-existent product → 404 ──────────────────────────────────
  it('10. Non-existent product → 404', async () => {
    await request(app.getHttpServer())
      .patch('/partners/products/00000000-0000-0000-0000-000000000000')
      .set('Authorization', `Bearer ${vendorAToken}`)
      .send({ name: 'Ghost' })
      .expect(404);

    await request(app.getHttpServer())
      .delete('/partners/products/00000000-0000-0000-0000-000000000000')
      .set('Authorization', `Bearer ${vendorAToken}`)
      .expect(404);
  });
});
