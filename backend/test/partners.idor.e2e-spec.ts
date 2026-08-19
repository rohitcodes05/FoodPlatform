import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from '../src/app.module';
import { PrismaService } from '../src/prisma/prisma.service';
import { JwtService } from '@nestjs/jwt';
import { Role, PartnerType, OrderStatus } from '../generated/prisma/client.js';

describe('Partner Orders IDOR Security (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService;
  let jwtService: JwtService;

  let partnerAToken: string;
  let partnerBToken: string;
  let customerToken: string;
  
  let partnerAId: string;
  let partnerBId: string;
  let orderAId: string;
  let orderBId: string;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();

    prisma = app.get(PrismaService);
    jwtService = app.get(JwtService);

    // Clean up
    await prisma.orderItem.deleteMany({});
    await prisma.order.deleteMany({});
    await prisma.product.deleteMany({});
    await prisma.fulfillmentPoint.deleteMany({});
    await prisma.partner.deleteMany({});
    await prisma.userRole.deleteMany({});
    await prisma.user.deleteMany({});

    // 1. Create Users
    const userA = await prisma.user.create({
      data: {
        email: 'partnerA@test.com', passwordHash: 'hash', name: 'Partner A',
        roles: { create: { role: 'VENDOR' as any } }
      }
    });
    const userB = await prisma.user.create({
      data: {
        email: 'partnerB@test.com', passwordHash: 'hash', name: 'Partner B',
        roles: { create: { role: 'VENDOR' as any } }
      }
    });
    const customer = await prisma.user.create({
      data: {
        email: 'customer@test.com', passwordHash: 'hash', name: 'Customer',
        roles: { create: { role: 'CUSTOMER' as any } }
      }
    });

    // Generate tokens
    partnerAToken = jwtService.sign({ sub: userA.id, email: userA.email });
    partnerBToken = jwtService.sign({ sub: userB.id, email: userB.email });
    customerToken = jwtService.sign({ sub: customer.id, email: customer.email });

    // 2. Create Partners
    const partnerA = await prisma.partner.create({
      data: { userId: userA.id, businessName: 'Biz A', type: 'RESTAURANT' as any }
    });
    partnerAId = partnerA.id;

    const partnerB = await prisma.partner.create({
      data: { userId: userB.id, businessName: 'Biz B', type: 'RESTAURANT' as any }
    });
    partnerBId = partnerB.id;

    // Fulfillment points
    const fpA = await prisma.fulfillmentPoint.create({
      data: { name: 'FP A', street: 'A', city: 'A', state: 'A', postalCode: 'A', country: 'A', partnerId: partnerAId }
    });
    const fpB = await prisma.fulfillmentPoint.create({
      data: { name: 'FP B', street: 'B', city: 'B', state: 'B', postalCode: 'B', country: 'B', partnerId: partnerBId }
    });

    // 3. Create Orders belonging to A and B
    const orderA = await prisma.order.create({
      data: {
        userId: customer.id,
        partnerId: partnerAId,
        fulfillmentPointId: fpA.id,
        totalAmount: 100,
        status: 'PENDING' as any
      }
    });
    orderAId = orderA.id;

    const orderB = await prisma.order.create({
      data: {
        userId: customer.id,
        partnerId: partnerBId,
        fulfillmentPointId: fpB.id,
        totalAmount: 200,
        status: 'PENDING' as any
      }
    });
    orderBId = orderB.id;
  });

  afterAll(async () => {
    await app.close();
  });

  it('1. Vendor A -> own order GET = allowed', async () => {
    const res = await request(app.getHttpServer())
      .get('/partners/orders')
      .set('Authorization', `Bearer ${partnerAToken}`)
      .expect(200);

    expect(res.body.length).toBe(1);
    expect(res.body[0].id).toBe(orderAId);
  });

  it('2. Vendor A -> Vendor B order GET/list = B ka order expose nahi hota', async () => {
    const res = await request(app.getHttpServer())
      .get('/partners/orders')
      .set('Authorization', `Bearer ${partnerAToken}`)
      .expect(200);

    const bOrderFound = res.body.some(o => o.id === orderBId);
    expect(bOrderFound).toBe(false);
  });

  it('3. Vendor A -> own order status update = allowed', async () => {
    const res = await request(app.getHttpServer())
      .patch(`/partners/orders/${orderAId}/status`)
      .set('Authorization', `Bearer ${partnerAToken}`)
      .send({ status: 'CONFIRMED' as any })
      .expect(200);
      
    expect(res.body.status).toBe('CONFIRMED' as any);
  });

  it('4. Vendor A -> Vendor B order status update = 403', async () => {
    const res = await request(app.getHttpServer())
      .patch(`/partners/orders/${orderBId}/status`)
      .set('Authorization', `Bearer ${partnerAToken}`)
      .send({ status: 'CONFIRMED' as any })
      .expect(403);
      
    expect(res.body.message).toContain('do not have permission');
  });

  it('5. Vendor B -> Vendor A order status update = 403', async () => {
    const res = await request(app.getHttpServer())
      .patch(`/partners/orders/${orderAId}/status`)
      .set('Authorization', `Bearer ${partnerBToken}`)
      .send({ status: 'PREPARING' as any })
      .expect(403);
      
    expect(res.body.message).toContain('do not have permission');
  });

  it('6. Customer -> /partners/orders = 403', async () => {
    await request(app.getHttpServer())
      .get('/partners/orders')
      .set('Authorization', `Bearer ${customerToken}`)
      .expect(403);
  });

  it('7. Unauthenticated -> /partners/orders = 401', async () => {
    await request(app.getHttpServer())
      .get('/partners/orders')
      .expect(401);
  });

  it('8. Invalid status transition = 400', async () => {
    // Order A is CONFIRMED. Moving to DELIVERED is invalid.
    const res = await request(app.getHttpServer())
      .patch(`/partners/orders/${orderAId}/status`)
      .set('Authorization', `Bearer ${partnerAToken}`)
      .send({ status: 'DELIVERED' as any })
      .expect(400);

    expect(res.body.message).toContain('Invalid status transition');
  });

  it('9. Non-existent order = 404', async () => {
    await request(app.getHttpServer())
      .patch(`/partners/orders/00000000-0000-0000-0000-000000000000/status`)
      .set('Authorization', `Bearer ${partnerAToken}`)
      .send({ status: 'CONFIRMED' as any })
      .expect(404);
  });
});


