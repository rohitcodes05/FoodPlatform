import { Controller, Post, Get, Patch, Delete, Body, Param, UseGuards } from '@nestjs/common';
import { PartnersService } from './partners.service';
import { CreatePartnerDto } from './dto/create-partner.dto';
import { UpdatePartnerOrderStatusDto } from './dto/update-order-status.dto';
import { CreatePartnerProductDto } from './dto/create-partner-product.dto';
import { UpdatePartnerProductDto } from './dto/update-partner-product.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { CurrentUser } from '../auth/current-user.decorator';
import type { User } from '../../generated/prisma/client.js';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { Role } from '../../generated/prisma/client.js';

@Controller('partners')
@UseGuards(JwtAuthGuard, RolesGuard)
export class PartnersController {
  constructor(private readonly partnersService: PartnersService) {}

  @Post()
  createPartner(@CurrentUser() user: User, @Body() createPartnerDto: CreatePartnerDto) {
    return this.partnersService.createPartner(user.id, createPartnerDto);
  }

  @Get('me')
  @Roles(Role.VENDOR)
  getMyPartnerProfile(@CurrentUser() user: User) {
    return this.partnersService.getMyPartnerProfile(user.id);
  }

  // ─── Orders ────────────────────────────────────────────────────────────────

  @Get('orders')
  @Roles(Role.VENDOR)
  getPartnerOrders(@CurrentUser() user: User) {
    return this.partnersService.getPartnerOrders(user.id);
  }

  @Patch('orders/:id/status')
  @Roles(Role.VENDOR)
  updatePartnerOrderStatus(
    @CurrentUser() user: User,
    @Param('id') orderId: string,
    @Body() updateDto: UpdatePartnerOrderStatusDto,
  ) {
    return this.partnersService.updatePartnerOrderStatus(user.id, orderId, updateDto);
  }

  // ─── Products ──────────────────────────────────────────────────────────────

  @Get('products')
  @Roles(Role.VENDOR)
  getPartnerProducts(@CurrentUser() user: User) {
    return this.partnersService.getPartnerProducts(user.id);
  }

  @Post('products')
  @Roles(Role.VENDOR)
  createPartnerProduct(@CurrentUser() user: User, @Body() dto: CreatePartnerProductDto) {
    return this.partnersService.createPartnerProduct(user.id, dto);
  }

  @Patch('products/:id')
  @Roles(Role.VENDOR)
  updatePartnerProduct(
    @CurrentUser() user: User,
    @Param('id') productId: string,
    @Body() dto: UpdatePartnerProductDto,
  ) {
    return this.partnersService.updatePartnerProduct(user.id, productId, dto);
  }

  @Delete('products/:id')
  @Roles(Role.VENDOR)
  deletePartnerProduct(@CurrentUser() user: User, @Param('id') productId: string) {
    return this.partnersService.deletePartnerProduct(user.id, productId);
  }
}
