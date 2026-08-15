import { Module } from '@nestjs/common';
import { AdminController } from './admin.controller';
import { ProductsModule } from '../products/products.module';
import { OrdersModule } from '../orders/orders.module';
import { DeliveriesModule } from '../deliveries/deliveries.module';
import { PrismaModule } from '../prisma/prisma.module';

@Module({
  imports: [ProductsModule, OrdersModule, DeliveriesModule, PrismaModule],
  controllers: [AdminController],
})
export class AdminModule {}
