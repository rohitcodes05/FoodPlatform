import { Injectable, ConflictException, NotFoundException, ForbiddenException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreatePartnerDto } from './dto/create-partner.dto';
import { UpdatePartnerOrderStatusDto } from './dto/update-order-status.dto';
import { CreatePartnerProductDto } from './dto/create-partner-product.dto';
import { UpdatePartnerProductDto } from './dto/update-partner-product.dto';
import { Role, OrderStatus } from '../../generated/prisma/client.js';

@Injectable()
export class PartnersService {
  constructor(private readonly prisma: PrismaService) {}

  async createPartner(userId: string, createPartnerDto: CreatePartnerDto) {
    const existingPartner = await this.prisma.partner.findUnique({
      where: { userId },
    });

    if (existingPartner) {
      throw new ConflictException('User already has a registered partner profile');
    }

    return this.prisma.$transaction(async (tx) => {
      const partner = await tx.partner.create({
        data: {
          userId,
          businessName: createPartnerDto.businessName,
          type: createPartnerDto.type,
        },
      });

      // Ensure the user has the VENDOR role.
      await tx.userRole.upsert({
        where: { userId_role: { userId, role: Role.VENDOR } },
        create: { userId, role: Role.VENDOR },
        update: {},
      });

      return partner;
    });
  }

  async getMyPartnerProfile(userId: string) {
    const partner = await this.prisma.partner.findUnique({
      where: { userId },
    });

    if (!partner) {
      throw new NotFoundException('Partner profile not found');
    }

    return partner;
  }

  async getPartnerOrders(userId: string) {
    const partner = await this.getMyPartnerProfile(userId);

    return this.prisma.order.findMany({
      where: { partnerId: partner.id },
      include: {
        items: { include: { product: true } },
        address: true,
        payment: true,
        delivery: true,
        user: { select: { id: true, name: true, phone: true, email: true } },
      },
      orderBy: { createdAt: 'desc' }
    });
  }

  async updatePartnerOrderStatus(userId: string, orderId: string, dto: UpdatePartnerOrderStatusDto) {
    const partner = await this.getMyPartnerProfile(userId);

    const order = await this.prisma.order.findUnique({
      where: { id: orderId }
    });

    if (!order) {
      throw new NotFoundException('Order not found');
    }

    if (order.partnerId !== partner.id) {
      throw new ForbiddenException('You do not have permission to modify this order');
    }

    const validTransitions: Record<OrderStatus, OrderStatus[]> = {
      PENDING: [OrderStatus.CONFIRMED, OrderStatus.CANCELLED],
      CONFIRMED: [OrderStatus.PREPARING],
      PREPARING: [OrderStatus.PACKED, OrderStatus.READY],
      PACKED: [OrderStatus.READY],
      READY: [],
      OUT_FOR_DELIVERY: [],
      DELIVERED: [],
      CANCELLED: [],
    };

    if (!validTransitions[order.status]?.includes(dto.status)) {
      throw new BadRequestException(`Invalid status transition from ${order.status} to ${dto.status}`);
    }

    return this.prisma.order.update({
      where: { id: orderId },
      data: { status: dto.status },
      include: {
        items: { include: { product: true } },
        address: true,
        payment: true,
        delivery: true,
        user: { select: { id: true, name: true, phone: true, email: true } },
      }
    });
  }

  // ─── Partner Product Management ────────────────────────────────────────────

  async getPartnerProducts(userId: string) {
    const partner = await this.getMyPartnerProfile(userId);

    return this.prisma.product.findMany({
      where: { partnerId: partner.id },
      include: { categories: { select: { id: true, name: true } } },
      orderBy: { createdAt: 'desc' },
    });
  }

  async createPartnerProduct(userId: string, dto: CreatePartnerProductDto) {
    const partner = await this.getMyPartnerProfile(userId);

    return this.prisma.product.create({
      data: {
        name: dto.name,
        description: dto.description,
        type: dto.type,
        price: dto.price,
        isAvailable: dto.isAvailable ?? true,
        // partnerId is ALWAYS derived from JWT — never from client payload
        partnerId: partner.id,
        categories: {
          connect: dto.categoryIds?.map((id) => ({ id })) ?? [],
        },
      },
      include: { categories: { select: { id: true, name: true } } },
    });
  }

  async updatePartnerProduct(userId: string, productId: string, dto: UpdatePartnerProductDto) {
    const partner = await this.getMyPartnerProfile(userId);

    const product = await this.prisma.product.findUnique({ where: { id: productId } });
    if (!product) throw new NotFoundException('Product not found');
    if (product.partnerId !== partner.id)
      throw new ForbiddenException('You do not have permission to modify this product');

    return this.prisma.product.update({
      where: { id: productId },
      data: {
        ...(dto.name !== undefined && { name: dto.name }),
        ...(dto.description !== undefined && { description: dto.description }),
        ...(dto.type !== undefined && { type: dto.type }),
        ...(dto.price !== undefined && { price: dto.price }),
        ...(dto.isAvailable !== undefined && { isAvailable: dto.isAvailable }),
        ...(dto.categoryIds !== undefined && {
          categories: { set: dto.categoryIds.map((id) => ({ id })) },
        }),
      },
      include: { categories: { select: { id: true, name: true } } },
    });
  }

  async deletePartnerProduct(userId: string, productId: string) {
    const partner = await this.getMyPartnerProfile(userId);

    const product = await this.prisma.product.findUnique({
      where: { id: productId },
      include: { _count: { select: { orderItems: true } } },
    });

    if (!product) throw new NotFoundException('Product not found');
    if (product.partnerId !== partner.id)
      throw new ForbiddenException('You do not have permission to delete this product');

    // Option A: hard delete only if no order history exists (schema: orderItems onDelete: Restrict)
    if (product._count.orderItems > 0) {
      throw new ConflictException('Product has existing orders and cannot be deleted');
    }

    await this.prisma.product.delete({ where: { id: productId } });
    return { message: 'Product deleted successfully' };
  }
}
