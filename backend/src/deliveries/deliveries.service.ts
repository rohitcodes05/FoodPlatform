import { Injectable, NotFoundException, ConflictException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { DeliveryStatus } from '../../generated/prisma/client.js';

@Injectable()
export class DeliveriesService {
  constructor(private readonly prisma: PrismaService) {}

  private readonly validTransitions: Record<DeliveryStatus, DeliveryStatus[]> = {
    [DeliveryStatus.PENDING]: [DeliveryStatus.PICKED_UP, DeliveryStatus.FAILED],
    [DeliveryStatus.PICKED_UP]: [DeliveryStatus.IN_TRANSIT, DeliveryStatus.FAILED],
    [DeliveryStatus.IN_TRANSIT]: [DeliveryStatus.DELIVERED, DeliveryStatus.FAILED],
    [DeliveryStatus.DELIVERED]: [],
    [DeliveryStatus.FAILED]: [],
  };

  async createDelivery(orderId: string) {
    const existing = await this.prisma.delivery.findUnique({ where: { orderId } });
    if (existing) {
      throw new ConflictException('Delivery record already exists for this order');
    }

    return this.prisma.delivery.create({
      data: {
        orderId,
        status: DeliveryStatus.PENDING,
      },
    });
  }

  async getDeliveryByOrderId(orderId: string) {
    const delivery = await this.prisma.delivery.findUnique({
      where: { orderId },
    });
    if (!delivery) {
      throw new NotFoundException(`Delivery for order ${orderId} not found`);
    }
    return delivery;
  }

  async updateDeliveryStatus(id: string, newStatus: DeliveryStatus, trackingCode?: string) {
    const delivery = await this.prisma.delivery.findUnique({ where: { id } });
    if (!delivery) {
      throw new NotFoundException(`Delivery ${id} not found`);
    }

    const allowed = this.validTransitions[delivery.status];
    if (!allowed.includes(newStatus)) {
      throw new BadRequestException(
        `Invalid status transition from ${delivery.status} to ${newStatus}`
      );
    }

    return this.prisma.delivery.update({
      where: { id },
      data: {
        status: newStatus,
        ...(trackingCode && { trackingCode }),
      },
    });
  }
}
