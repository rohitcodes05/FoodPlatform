import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { PaymentStatus, Prisma } from '../../generated/prisma/client.js';

@Injectable()
export class PaymentsService {
  constructor(private readonly prisma: PrismaService) {}

  async createPayment(orderId: string, amount: Prisma.Decimal | number, provider?: string, transactionId?: string) {
    const existing = await this.prisma.payment.findUnique({ where: { orderId } });
    if (existing) {
      throw new ConflictException('Payment record already exists for this order');
    }

    return this.prisma.payment.create({
      data: {
        orderId,
        amount,
        status: PaymentStatus.PENDING,
        provider,
        transactionId,
      },
    });
  }

  async getPaymentByOrderId(orderId: string) {
    const payment = await this.prisma.payment.findUnique({
      where: { orderId },
    });
    if (!payment) {
      throw new NotFoundException(`Payment for order ${orderId} not found`);
    }
    return payment;
  }

  async updatePaymentStatus(id: string, status: PaymentStatus) {
    return this.prisma.payment.update({
      where: { id },
      data: { status },
    });
  }
}
