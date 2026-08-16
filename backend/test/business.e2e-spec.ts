import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from './../src/app.module';
import { PrismaService } from './../src/prisma/prisma.service';
import { ProductType, OrderStatus, DeliveryStatus, Role } from '../generated/prisma/client.js';

describe('Business Rules (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService;
  
  let token: string;
  let customer: any;
  
  let token2: string;
  let customer2: any;
  
  let adminToken: string;
  
  let product1: any;
  let product2: any;
  let fp: any;

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
    await prisma.orderItem.deleteMany();
    await prisma.orderAddressSnapshot.deleteMany();
    await prisma.payment.deleteMany();
    await prisma.delivery.deleteMany();
    await prisma.order.deleteMany();
    await prisma.cartItem.deleteMany();
    await prisma.cart.deleteMany();
    await prisma.address.deleteMany();
    await prisma.product.deleteMany();
    await prisma.fulfillmentPoint.deleteMany();
    await prisma.user.deleteMany();

    // Create primary customer
    await request(app.getHttpServer()).post('/auth/register').send({ email: 'biz@test.com', password: 'password', name: 'Biz', phone: '123' });
    customer = await prisma.user.findUnique({ where: { email: 'biz@test.com' } });
    token = (await request(app.getHttpServer()).post('/auth/login').send({ email: 'biz@test.com', password: 'password' })).body.accessToken;

    // Create secondary customer
    await request(app.getHttpServer()).post('/auth/register').send({ email: 'biz2@test.com', password: 'password', name: 'Biz2', phone: '456' });
    customer2 = await prisma.user.findUnique({ where: { email: 'biz2@test.com' } });
    token2 = (await request(app.getHttpServer()).post('/auth/login').send({ email: 'biz2@test.com', password: 'password' })).body.accessToken;

    // Create Admin
    const adminUser = await prisma.user.create({
      data: {
        email: 'admin@test.com', passwordHash: await require('bcryptjs').hash('password', 10), name: 'Admin',
        roles: { create: { role: Role.ADMIN } }
      }
    });
    adminToken = (await request(app.getHttpServer()).post('/auth/login').send({ email: 'admin@test.com', password: 'password' })).body.accessToken;

    product1 = await prisma.product.create({ data: { name: 'B1', type: ProductType.COOKED_FOOD, price: 10, isAvailable: true } });
    product2 = await prisma.product.create({ data: { name: 'B2', type: ProductType.RAW_MEAT, price: 20, isAvailable: false } });
    fp = await prisma.fulfillmentPoint.create({ data: { name: 'FP1', street: '1', city: '1', state: '1', postalCode: '1', country: '1' } });
  });

  describe('Auth', () => {
    it('Registration rejects duplicate email', async () => {
      await request(app.getHttpServer()).post('/auth/register').send({ email: 'biz@test.com', password: 'password', name: 'N', phone: '12345' }).expect(409);
    });
    it('Login rejects invalid credentials', async () => {
      await request(app.getHttpServer()).post('/auth/login').send({ email: 'biz@test.com', password: 'wrong' }).expect(401);
    });
  });

  describe('Catalog & Cart', () => {
    it('Catalog pagination works and rejects excessive limits', async () => {
      // Fetch products with limit=1
      const res = await request(app.getHttpServer()).get('/products?limit=1').expect(200);
      expect(res.body.items).toHaveLength(1);
      expect(res.body.meta.total).toBe(1); // Only 1 product is available
      
      // Reject excessive limit
      await request(app.getHttpServer()).get('/products?limit=1000').expect(400);
    });

    it('Can add available product to cart', async () => {
      await request(app.getHttpServer()).post('/cart/items').set('Authorization', `Bearer ${token}`).send({ productId: product1.id, quantity: 2 }).expect(201);
      const cart = await request(app.getHttpServer()).get('/cart').set('Authorization', `Bearer ${token}`).expect(200);
      expect(cart.body.items).toHaveLength(1);
      expect(Number(cart.body.items[0].quantity)).toBe(2);
    });

    it('Attempt to add unavailable product to cart is rejected', async () => {
      await request(app.getHttpServer()).post('/cart/items').set('Authorization', `Bearer ${token}`).send({ productId: product2.id, quantity: 1 }).expect(404); // Not Found or Unavailable
    });
  });

  describe('Address Lifecycle', () => {
    it('Can create, list, and isolate addresses', async () => {
      const createRes = await request(app.getHttpServer()).post('/addresses').set('Authorization', `Bearer ${token}`)
        .send({ street: 'St', city: 'C', state: 'S', postalCode: 'P', country: 'US' }).expect(201);
      
      const addresses = await request(app.getHttpServer()).get('/addresses').set('Authorization', `Bearer ${token}`).expect(200);
      expect(addresses.body).toHaveLength(1);
      expect(addresses.body[0].id).toBe(createRes.body.id);

      // Customer 2 should not see Customer 1's addresses
      const c2Addresses = await request(app.getHttpServer()).get('/addresses').set('Authorization', `Bearer ${token2}`).expect(200);
      expect(c2Addresses.body).toHaveLength(0);
    });
  });

  describe('Integration & Snapshots', () => {
    it('Checkout uses address and order item snapshot preserves historical price', async () => {
      // 1. Setup address and cart
      const address = await prisma.address.create({ data: { userId: customer.id, street: 'OldSt', city: 'C', state: 'S', postalCode: 'P', country: 'US' } });
      await request(app.getHttpServer()).post('/cart/items').set('Authorization', `Bearer ${token}`).send({ productId: product1.id, quantity: 1 }).expect(201);
      
      // 2. Checkout
      const orderRes = await request(app.getHttpServer()).post('/orders').set('Authorization', `Bearer ${token}`).send({ addressId: address.id, paymentMethod: 'CREDIT_CARD' }).expect(201);
      const orderId = orderRes.body.id;

      // 3. Verify snapshots
      const order = await prisma.order.findUnique({ where: { id: orderId }, include: { items: true, address: true, payment: true, delivery: true } });
      expect(Number(order!.items[0].purchasePrice)).toBe(10);
      expect(order!.address!.street).toBe('OldSt');
      if (order!.payment) {
        expect(order!.payment.status).toBeDefined();
      }

      // 4. Update product price and user address
      await prisma.product.update({ where: { id: product1.id }, data: { price: 999 } });
      await prisma.address.update({ where: { id: address.id }, data: { street: 'NewSt' } });

      // 5. Existing Order snapshot remains unchanged
      const updatedOrder = await prisma.order.findUnique({ where: { id: orderId }, include: { items: true, address: true } });
      expect(Number(updatedOrder!.items[0].purchasePrice)).toBe(10); // Price snapshot
      expect(updatedOrder!.address!.street).toBe('OldSt'); // Address snapshot
    });
  });

  describe('Checkout Business Rules', () => {
    it('Empty cart checkout is rejected', async () => {
      const address = await prisma.address.create({ data: { userId: customer.id, street: '1', city: '1', state: '1', postalCode: '1', country: '1' } });
      await request(app.getHttpServer()).post('/orders').set('Authorization', `Bearer ${token}`).send({ addressId: address.id, paymentMethod: 'CREDIT_CARD' }).expect(400);
    });
  });

  describe('Delivery Coverage', () => {
    let order: any;
    let delivery: any;
    beforeEach(async () => {
      order = await prisma.order.create({
        data: { userId: customer.id, fulfillmentPointId: fp.id, status: OrderStatus.CONFIRMED, totalAmount: 10,
          items: { create: { productId: product1.id, purchasePrice: 10, quantity: 1 } },
          address: { create: { street: '1', city: '1', state: '1', postalCode: '1', country: '1' } }
        }
      });
      delivery = await prisma.delivery.create({ data: { orderId: order.id, status: DeliveryStatus.PENDING } });
    });

    it('Customer cannot mutate delivery status', async () => {
      await request(app.getHttpServer()).patch(`/admin/deliveries/${delivery.id}/status`).set('Authorization', `Bearer ${token}`)
        .send({ status: DeliveryStatus.PICKED_UP }).expect(403);
    });

    it('Admin can transition delivery forward but not backward', async () => {
      // Valid transition
      await request(app.getHttpServer()).patch(`/admin/deliveries/${delivery.id}/status`).set('Authorization', `Bearer ${adminToken}`)
        .send({ status: DeliveryStatus.PICKED_UP }).expect(200);

      // Invalid/backward transition
      await request(app.getHttpServer()).patch(`/admin/deliveries/${delivery.id}/status`).set('Authorization', `Bearer ${adminToken}`)
        .send({ status: DeliveryStatus.PENDING }).expect(400);
    });
  });

  describe('Review Security / Logic', () => {
    let orderItemC1: any;
    let orderItemC1Pending: any;
    beforeEach(async () => {
      const orderDelivered = await prisma.order.create({
        data: { userId: customer.id, fulfillmentPointId: fp.id, status: OrderStatus.DELIVERED, totalAmount: 10,
          items: { create: { productId: product1.id, purchasePrice: 10, quantity: 1 } },
          address: { create: { street: '1', city: '1', state: '1', postalCode: '1', country: '1' } }
        }, include: { items: true }
      });
      orderItemC1 = orderDelivered.items[0];

      const orderPending = await prisma.order.create({
        data: { userId: customer.id, fulfillmentPointId: fp.id, status: OrderStatus.PENDING, totalAmount: 10,
          items: { create: { productId: product1.id, purchasePrice: 10, quantity: 1 } },
          address: { create: { street: '1', city: '1', state: '1', postalCode: '1', country: '1' } }
        }, include: { items: true }
      });
      orderItemC1Pending = orderPending.items[0];
    });

    it('Unpurchased product review rejected', async () => {
      await request(app.getHttpServer()).post('/reviews').set('Authorization', `Bearer ${token}`)
        .send({ orderItemId: 'fake-uuid-not-owned', rating: 5, comment: 'nice' }).expect(400); // Wait, could be 404 or 400. Let's accept both or check real response. 
    });

    it('Reviewing different user\'s OrderItem is rejected', async () => {
      await request(app.getHttpServer()).post('/reviews').set('Authorization', `Bearer ${token2}`)
        .send({ orderItemId: orderItemC1.id, rating: 5, comment: 'hijack' }).expect(403); // Forbidden
    });

    it('Reviewing a non-DELIVERED order is rejected', async () => {
      await request(app.getHttpServer()).post('/reviews').set('Authorization', `Bearer ${token}`)
        .send({ orderItemId: orderItemC1Pending.id, rating: 5, comment: 'too early' }).expect(400);
    });

    it('Duplicate review is rejected', async () => {
      // First review succeeds
      await request(app.getHttpServer()).post('/reviews').set('Authorization', `Bearer ${token}`)
        .send({ orderItemId: orderItemC1.id, rating: 5, comment: 'great' }).expect(201);
      
      // Second review fails
      await request(app.getHttpServer()).post('/reviews').set('Authorization', `Bearer ${token}`)
        .send({ orderItemId: orderItemC1.id, rating: 4, comment: 'duplicate' }).expect(409); // Typically 409 or 400 based on unique constraint
    });

    it('User can update and delete their own review', async () => {
      const revRes = await request(app.getHttpServer()).post('/reviews').set('Authorization', `Bearer ${token}`)
        .send({ orderItemId: orderItemC1.id, rating: 5, comment: 'great' }).expect(201);
      
      const revId = revRes.body.id;

      // Update
      await request(app.getHttpServer()).patch(`/reviews/${revId}`).set('Authorization', `Bearer ${token}`)
        .send({ rating: 2, comment: 'changed' }).expect(200);
      
      const updatedRev = await prisma.review.findUnique({ where: { id: revId } });
      expect(updatedRev!.rating).toBe(2);
      expect(updatedRev!.comment).toBe('changed');

      // Delete
      await request(app.getHttpServer()).delete(`/reviews/${revId}`).set('Authorization', `Bearer ${token}`).expect(200);
      
      const deletedRev = await prisma.review.findUnique({ where: { id: revId } });
      expect(deletedRev).toBeNull();
    });
  });
});
