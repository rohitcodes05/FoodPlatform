import { Injectable, ConflictException, NotFoundException, ForbiddenException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreatePartnerDto } from './dto/create-partner.dto';
import { UpdatePartnerOrderStatusDto } from './dto/update-order-status.dto';
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
}
