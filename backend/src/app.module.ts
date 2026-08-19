import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaModule } from './prisma/prisma.module.js';
import { HealthModule } from './health/health.module.js';
import { ProductsModule } from './products/products.module';
import { CategoriesModule } from './categories/categories.module';
import { CartModule } from './cart/cart.module';
import { AuthModule } from './auth/auth.module';
import { AddressesModule } from './addresses/addresses.module';
import { OrdersModule } from './orders/orders.module';
import { PaymentsModule } from './payments/payments.module';
import { DeliveriesModule } from './deliveries/deliveries.module';
import { ReviewsModule } from './reviews/reviews.module';
import { AdminModule } from './admin/admin.module';
import { PartnersModule } from './partners/partners.module';

@Module({
  imports: [PrismaModule, HealthModule, ProductsModule, CategoriesModule, CartModule, AuthModule, AddressesModule, OrdersModule, PaymentsModule, DeliveriesModule, ReviewsModule, AdminModule, PartnersModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
