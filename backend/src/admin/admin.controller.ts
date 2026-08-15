import { Controller, Get, Post, Patch, Body, Param, UseGuards } from '@nestjs/common';
import { ProductsService } from '../products/products.service';
import { OrdersService } from '../orders/orders.service';
import { DeliveriesService } from '../deliveries/deliveries.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { Role } from '../../generated/prisma/client.js';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { UpdateOrderStatusDto } from './dto/update-order-status.dto';
import { UpdateDeliveryStatusDto } from './dto/update-delivery-status.dto';

@Controller('admin')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(Role.ADMIN)
export class AdminController {
  constructor(
    private readonly productsService: ProductsService,
    private readonly ordersService: OrdersService,
    private readonly deliveriesService: DeliveriesService,
  ) {}

  // Products
  @Post('products')
  createProduct(@Body() createProductDto: CreateProductDto) {
    return this.productsService.createProduct(createProductDto);
  }

  @Patch('products/:id')
  updateProduct(@Param('id') id: string, @Body() updateProductDto: UpdateProductDto) {
    return this.productsService.updateProduct(id, updateProductDto);
  }

  // Orders
  @Get('orders')
  getOrders() {
    return this.ordersService.findAllAdmin();
  }

  @Patch('orders/:id/status')
  updateOrderStatus(@Param('id') id: string, @Body() updateOrderStatusDto: UpdateOrderStatusDto) {
    return this.ordersService.updateOrderStatus(id, updateOrderStatusDto.status);
  }

  // Deliveries
  @Get('deliveries')
  getDeliveries() {
    return this.deliveriesService.findAllAdmin();
  }

  @Patch('deliveries/:id/status')
  updateDeliveryStatus(@Param('id') id: string, @Body() updateDeliveryStatusDto: UpdateDeliveryStatusDto) {
    return this.deliveriesService.updateDeliveryStatus(id, updateDeliveryStatusDto.status, updateDeliveryStatusDto.trackingCode);
  }
}
