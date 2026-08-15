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

@Module({
  imports: [PrismaModule, HealthModule, ProductsModule, CategoriesModule, CartModule, AuthModule, AddressesModule, OrdersModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
