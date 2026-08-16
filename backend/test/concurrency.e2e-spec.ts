import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from './../src/app.module';
import { PrismaService } from './../src/prisma/prisma.service';
import { ProductType } from '../generated/prisma/client.js';

describe('Concurrency (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService;
  
  let customer: any;
  let token: string;
  let product: any;

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
    await prisma.user.deleteMany();

    await request(app.getHttpServer()).post('/auth/register').send({ email: 'conc@test.com', password: 'password', name: 'Conc', phone: '123' });
    customer = await prisma.user.findUnique({ where: { email: 'conc@test.com' } });
    token = (await request(app.getHttpServer()).post('/auth/login').send({ email: 'conc@test.com', password: 'password' })).body.accessToken;

    product = await prisma.product.create({ data: { name: 'Limited Product', type: ProductType.COOKED_FOOD, price: 10, isAvailable: true } });
    
    await request(app.getHttpServer())
      .post('/cart/items')
      .set('Authorization', `Bearer ${token}`)
      .send({ productId: product.id, quantity: 1 })
      .expect(201);
      
    await prisma.address.create({ data: { userId: customer.id, street: '1', city: '1', state: '1', postalCode: '1', country: '1' } });
  });

  describe('Checkout Concurrency', () => {
    it('Promise.all: Duplicate checkout requests result in exactly one successful order', async () => {
      const addressId = (await prisma.address.findFirst({ where: { userId: customer.id } }))?.id;

      // Ensure the cart has items initially
      const cartBefore = await prisma.cart.findUnique({ where: { userId: customer.id }, include: { items: true } });
      expect(cartBefore?.items).toHaveLength(1);

      // Issue 3 simultaneous checkout requests!
      const requests = [
        request(app.getHttpServer()).post('/orders').set('Authorization', `Bearer ${token}`).send({ addressId, paymentMethod: 'CREDIT_CARD' }),
        request(app.getHttpServer()).post('/orders').set('Authorization', `Bearer ${token}`).send({ addressId, paymentMethod: 'CREDIT_CARD' }),
        request(app.getHttpServer()).post('/orders').set('Authorization', `Bearer ${token}`).send({ addressId, paymentMethod: 'CREDIT_CARD' })
      ];

      const responses = await Promise.all(requests);
      
      const successResponses = responses.filter(r => r.status === 201);
      const failResponses = responses.filter(r => r.status === 400 || r.status === 409 || r.status === 500);

      if (successResponses.length === 0) {
        console.log('ALL FAILED! Responses:', responses.map(r => r.body));
      } else {
        console.log('FAIL RESPONSES:', failResponses.map(r => r.body));
      }

      // ONLY ONE MUST SUCCEED
      expect(successResponses.length).toBe(1);
      expect(failResponses.length).toBe(2);

      // Verify the cart is now empty
      const cartAfter = await prisma.cart.findUnique({ where: { userId: customer.id }, include: { items: true } });
      expect(cartAfter?.items).toHaveLength(0);

      // Verify only 1 order exists for this user in DB
      const orders = await prisma.order.findMany({ where: { userId: customer.id } });
      expect(orders).toHaveLength(1);
    });
  });
});
