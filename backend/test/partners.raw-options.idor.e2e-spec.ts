import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from '../src/app.module';
import { PrismaService } from '../src/prisma/prisma.service';
import { PartnerType, ProductType, Role } from '../generated/prisma/client.js';

describe('Partner Raw Options IDOR (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService;
  
  let userA: any;
  let userB: any;
  let tokenA: string;
  let tokenB: string;
  let productA: any;
  let productB: any;
  let cutA: any;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalPipes(new ValidationPipe({ whitelist: true }));
    await app.init();
    
    prisma = app.get(PrismaService);
    await prisma.cartItem.deleteMany();
    await prisma.orderItem.deleteMany();
    await prisma.product.deleteMany();
    await prisma.order.deleteMany(); await prisma.fulfillmentPoint.deleteMany(); await prisma.partner.deleteMany();
    await prisma.userRole.deleteMany();
    await prisma.user.deleteMany();

    // Create User A & Partner A
    userA = await prisma.user.create({
      data: {
        email: 'raw_a@example.com',
        passwordHash: 'hashed',
        name: 'Raw Seller A',
        phone: '1111111111',
        roles: { create: { role: 'VENDOR' as any } },
        partner: {
          create: {
            businessName: 'Vendor A Meat',
            type: 'RAW_SELLER' as any,
          }
        }
      },
      include: { partner: true }
    });

    // Create User B & Partner B
    userB = await prisma.user.create({
      data: {
        email: 'raw_b@example.com',
        passwordHash: 'hashed',
        name: 'Raw Seller B',
        phone: '2222222222',
        roles: { create: { role: 'VENDOR' as any } },
        partner: {
          create: {
            businessName: 'Vendor B Meat',
            type: 'RAW_SELLER' as any,
          }
        }
      },
      include: { partner: true }
    });

    const jwtService = app.get(require('@nestjs/jwt').JwtService);
    tokenA = jwtService.sign({ sub: userA.id, email: userA.email, roles: ['VENDOR'] });
    tokenB = jwtService.sign({ sub: userB.id, email: userB.email, roles: ['VENDOR'] });

    // Create Products
    productA = await prisma.product.create({
      data: { name: 'Chicken A', type: 'RAW_MEAT' as any, price: 500, partnerId: userA.partner.id }
    });
    productB = await prisma.product.create({
      data: { name: 'Chicken B', type: 'RAW_MEAT' as any, price: 600, partnerId: userB.partner.id }
    });
  });

  afterAll(async () => {
    await prisma.cartItem.deleteMany();
    await prisma.orderItem.deleteMany();
    await prisma.weightOption.deleteMany();
    await prisma.cutOption.deleteMany();
    await prisma.product.deleteMany();
    await prisma.order.deleteMany(); await prisma.fulfillmentPoint.deleteMany(); await prisma.partner.deleteMany();
    await prisma.userRole.deleteMany();
    await prisma.user.deleteMany();
    await app.close();
  });

  it('RAW_SELLER can create cut option for owned product', async () => {
    const res = await request(app.getHttpServer())
      .post(`/partners/products/${productA.id}/cuts`)
      .set('Authorization', `Bearer ${tokenA}`)
      .send({ name: 'Curry Cut', isAvailable: true });
    
    expect(res.status).toBe(201);
    expect(res.body.name).toBe('Curry Cut');
    expect(res.body.productId).toBe(productA.id);
    cutA = res.body;
  });

  it('RAW_SELLER cannot add cut option to another partner product', async () => {
    const res = await request(app.getHttpServer())
      .post(`/partners/products/${productB.id}/cuts`)
      .set('Authorization', `Bearer ${tokenA}`)
      .send({ name: 'Hacked Cut', isAvailable: true });
    
    expect(res.status).toBe(403);
  });

  it('RAW_SELLER cannot modify another partner cut option', async () => {
    // A tries to patch A's cut using B's product ID -> 404/403
    let res = await request(app.getHttpServer())
      .patch(`/partners/products/${productB.id}/cuts/${cutA.id}`)
      .set('Authorization', `Bearer ${tokenA}`)
      .send({ name: 'Hacked Name' });
    expect(res.status).toBe(403);

    // B tries to patch A's cut
    res = await request(app.getHttpServer())
      .patch(`/partners/products/${productA.id}/cuts/${cutA.id}`)
      .set('Authorization', `Bearer ${tokenB}`)
      .send({ name: 'Hacked Name' });
    expect(res.status).toBe(403);
  });

  it('RAW_SELLER can create weight option', async () => {
    const res = await request(app.getHttpServer())
      .post(`/partners/products/${productA.id}/weights`)
      .set('Authorization', `Bearer ${tokenA}`)
      .send({ weightLabel: '500g', priceOverride: 250, isAvailable: true });
    
    expect(res.status).toBe(201);
    expect(res.body.weightLabel).toBe('500g');
    expect(Number(res.body.priceOverride)).toBe(250);
  });

  it('Customer cannot add RAW_MEAT without mandatory cut/weight if configured', async () => {
    const res = await request(app.getHttpServer())
      .post('/cart/items')
      .set('Authorization', `Bearer ${tokenA}`) // using A as a customer here
      .send({ productId: productA.id, quantity: 1 });
    
    expect(res.status).toBe(400);
    expect(res.body.message).toContain('cut option is required');
  });
});
