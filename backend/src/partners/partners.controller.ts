import { CreateCutOptionDto, UpdateCutOptionDto, CreateWeightOptionDto, UpdateWeightOptionDto } from './dto/variation-options.dto';
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
  deletePartnerProduct(@CurrentUser() user: User, @Param('id') id: string) {
    return this.partnersService.deletePartnerProduct(user.id, id);
  }

  // ── Raw Seller Variation Management (Cuts) ──────────────────────────────────

  @Post('products/:id/cuts')
  @Roles(Role.VENDOR)
  createCutOption(
    @CurrentUser() user: User,
    @Param('id') productId: string,
    @Body() dto: CreateCutOptionDto,
  ) {
    return this.partnersService.createCutOption(user.id, productId, dto.name, dto.isAvailable);
  }

  @Patch('products/:id/cuts/:cutId')
  @Roles(Role.VENDOR)
  updateCutOption(
    @CurrentUser() user: User,
    @Param('id') productId: string,
    @Param('cutId') cutId: string,
    @Body() dto: UpdateCutOptionDto,
  ) {
    return this.partnersService.updateCutOption(user.id, productId, cutId, dto.name, dto.isAvailable);
  }

  @Delete('products/:id/cuts/:cutId')
  @Roles(Role.VENDOR)
  deleteCutOption(
    @CurrentUser() user: User,
    @Param('id') productId: string,
    @Param('cutId') cutId: string,
  ) {
    return this.partnersService.deleteCutOption(user.id, productId, cutId);
  }

  // ── Raw Seller Variation Management (Weights) ───────────────────────────────

  @Post('products/:id/weights')
  @Roles(Role.VENDOR)
  createWeightOption(
    @CurrentUser() user: User,
    @Param('id') productId: string,
    @Body() dto: CreateWeightOptionDto,
  ) {
    return this.partnersService.createWeightOption(user.id, productId, dto.weightLabel, dto.priceOverride, dto.isAvailable);
  }

  @Patch('products/:id/weights/:weightId')
  @Roles(Role.VENDOR)
  updateWeightOption(
    @CurrentUser() user: User,
    @Param('id') productId: string,
    @Param('weightId') weightId: string,
    @Body() dto: UpdateWeightOptionDto,
  ) {
    return this.partnersService.updateWeightOption(user.id, productId, weightId, dto.weightLabel, dto.priceOverride, dto.isAvailable);
  }

  @Delete('products/:id/weights/:weightId')
  @Roles(Role.VENDOR)
  deleteWeightOption(
    @CurrentUser() user: User,
    @Param('id') productId: string,
    @Param('weightId') weightId: string,
  ) {
    return this.partnersService.deleteWeightOption(user.id, productId, weightId);
  }
}
