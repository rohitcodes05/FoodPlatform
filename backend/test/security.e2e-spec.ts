import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from './../src/app.module';
import { PrismaService } from './../src/prisma/prisma.service';
import { Role, ProductType, OrderStatus, DeliveryStatus } from '../generated/prisma/client.js';

describe('Security (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService;
  
  let customer1: any;
  let tokenC1: string;
  let customer2: any;
  let tokenC2: string;
  let admin: any;
  let tokenAdmin: string;

  let product: any;
  let fp: any;
  let address1: any;
  let order1: any;
  let review1: any;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalPipes(new ValidationPipe({ transform: true }));
    await app.init();
    
    prisma = app.get<PrismaService>(PrismaService);
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(async () => {
    await prisma.userRole.deleteMany();
    await prisma.review.deleteMany();
    await prisma.delivery.deleteMany();
    await prisma.payment.deleteMany();
    await prisma.orderItem.deleteMany();
    await prisma.orderAddressSnapshot.deleteMany();
    await prisma.order.deleteMany();
    await prisma.cartItem.deleteMany();
    await prisma.cart.deleteMany();
    await prisma.address.deleteMany();
    await prisma.product.deleteMany();
    await prisma.fulfillmentPoint.deleteMany();
    await prisma.user.deleteMany();

    // Create 2 customers and 1 admin via API to get real tokens
    const r1 = await request(app.getHttpServer()).post('/auth/register').send({ email: 'c1@test.com', password: 'password', name: 'C1', phone: '123' });
    if (r1.status !== 201) throw new Error('C1 registration failed: ' + JSON.stringify(r1.body));
    customer1 = await prisma.user.findUnique({ where: { email: 'c1@test.com' } });
    tokenC1 = (await request(app.getHttpServer()).post('/auth/login').send({ email: 'c1@test.com', password: 'password' })).body.accessToken;

    const r2 = await request(app.getHttpServer()).post('/auth/register').send({ email: 'c2@test.com', password: 'password', name: 'C2', phone: '456' });
    if (r2.status !== 201) throw new Error('C2 registration failed: ' + JSON.stringify(r2.body));
    customer2 = await prisma.user.findUnique({ where: { email: 'c2@test.com' } });
    tokenC2 = (await request(app.getHttpServer()).post('/auth/login').send({ email: 'c2@test.com', password: 'password' })).body.accessToken;

    // Create admin via prisma directly (since register gives CUSTOMER)
    const adminUser = await prisma.user.create({
      data: {
        email: 'admin@test.com', passwordHash: await require('bcryptjs').hash('password', 10), name: 'Admin',
        roles: { create: { role: Role.ADMIN } }
      }
    });
    admin = adminUser;
    tokenAdmin = (await request(app.getHttpServer()).post('/auth/login').send({ email: 'admin@test.com', password: 'password' })).body.accessToken;

    // Create data
    address1 = await prisma.address.create({ data: { userId: customer1.id, street: '1', city: '1', state: '1', postalCode: '1', country: '1' } });
    fp = await prisma.fulfillmentPoint.create({ data: { name: 'fp', street: '1', city: '1', state: '1', postalCode: '1', country: '1' } });
    product = await prisma.product.create({ data: { name: 'P1', type: ProductType.COOKED_FOOD, price: 10 } });
    
    order1 = await prisma.order.create({
      data: {
        userId: customer1.id, fulfillmentPointId: fp.id, status: OrderStatus.DELIVERED, totalAmount: 10,
        items: { create: { productId: product.id, purchasePrice: 10, quantity: 1 } },
        address: { create: { street: '1', city: '1', state: '1', postalCode: '1', country: '1' } }
      },
      include: { items: true }
    });

    review1 = await prisma.review.create({
      data: { userId: customer1.id, orderItemId: order1.items[0].id, rating: 5, comment: 'good' }
    });
  });

  describe('IDOR & Security Boundaries', () => {
    it('User A cannot access User B order', async () => {
      // C1 gets own order -> 200
      await request(app.getHttpServer()).get(`/orders/${order1.id}`).set('Authorization', `Bearer ${tokenC1}`).expect(200);
      // C2 gets C1's order -> 404 Not Found
      await request(app.getHttpServer()).get(`/orders/${order1.id}`).set('Authorization', `Bearer ${tokenC2}`).expect(404);
    });

    it('User A cannot modify User B address', async () => {
      // C2 deletes C1's address -> 404
      await request(app.getHttpServer()).delete(`/addresses/${address1.id}`).set('Authorization', `Bearer ${tokenC2}`).expect(404);
    });

    it('User A cannot modify User B cart (derive identity from JWT)', async () => {
      // C1 cart
      await request(app.getHttpServer()).post(`/cart/items`).set('Authorization', `Bearer ${tokenC1}`).send({ productId: product.id, quantity: 1 }).expect(201);
      const c2Cart = await request(app.getHttpServer()).get(`/cart`).set('Authorization', `Bearer ${tokenC2}`).expect(200);
      expect(c2Cart.body.items).toHaveLength(0); // Cart isolation
    });

    it('User A cannot modify User B review', async () => {
      await request(app.getHttpServer()).patch(`/reviews/${review1.id}`).set('Authorization', `Bearer ${tokenC2}`).send({ rating: 4 }).expect(403);
      await request(app.getHttpServer()).delete(`/reviews/${review1.id}`).set('Authorization', `Bearer ${tokenC2}`).expect(403);
    });
  });

  describe('Admin RBAC & Dynamic Revocation', () => {
    it('Requires valid auth for Admin API', async () => {
      await request(app.getHttpServer()).get('/admin/orders').expect(401);
      await request(app.getHttpServer()).get('/admin/orders').set('Authorization', 'Bearer invalid').expect(401);
    });

    it('Customer cannot access Admin endpoints', async () => {
      await request(app.getHttpServer()).get('/admin/orders').set('Authorization', `Bearer ${tokenC1}`).expect(403);
    });

    it('Fake role in request does not bypass RBAC', async () => {
      await request(app.getHttpServer()).get('/admin/orders?role=ADMIN').set('Authorization', `Bearer ${tokenC1}`).send({ role: 'ADMIN' }).expect(403);
    });

    it('Admin can access Admin endpoints', async () => {
      await request(app.getHttpServer()).get('/admin/orders').set('Authorization', `Bearer ${tokenAdmin}`).expect(200);
    });

    it('Admin role dynamic revocation works immediately', async () => {
      // Remove admin role from DB
      await prisma.userRole.deleteMany({ where: { userId: admin.id, role: Role.ADMIN } });
      // Same valid JWT should now be 403
      await request(app.getHttpServer()).get('/admin/orders').set('Authorization', `Bearer ${tokenAdmin}`).expect(403);
    });

    it('Admin Mutation Isolation: Customer cannot create products', async () => {
      await request(app.getHttpServer())
        .post('/admin/products')
        .set('Authorization', `Bearer ${tokenC1}`)
        .send({ name: 'Hacked', type: ProductType.COOKED_FOOD, price: 0 })
        .expect(403);
    });
  });
});
