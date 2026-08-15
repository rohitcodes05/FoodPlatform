import { Injectable, BadRequestException, NotFoundException, InternalServerErrorException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateOrderDto } from './dto/create-order.dto';
import { Prisma } from '../../generated/prisma/client.js';

@Injectable()
export class OrdersService {
  constructor(private readonly prisma: PrismaService) {}

  async create(userId: string, createOrderDto: CreateOrderDto) {
    const maxRetries = 3;
    let attempt = 0;

    while (attempt < maxRetries) {
      try {
        return await this.prisma.$transaction(async (tx) => {
          // 1. Fetch Cart
          const cart = await tx.cart.findUnique({
            where: { userId },
            include: { items: { include: { product: true } } },
          });
          if (!cart || cart.items.length === 0) {
            throw new BadRequestException('Cart is empty');
          }

          // 2. Validate Address (must belong to the user)
          const address = await tx.address.findUnique({
            where: { id: createOrderDto.addressId },
          });
          if (!address || address.userId !== userId) {
            throw new NotFoundException('Address not found');
          }

          // 3. Fetch active FulfillmentPoint
          const fulfillmentPoint = await tx.fulfillmentPoint.findFirst({
            where: { isActive: true },
          });
          if (!fulfillmentPoint) {
            throw new InternalServerErrorException('No active fulfillment point available');
          }

          // 4. Validate Products & Calculate Total
          let totalAmount = new Prisma.Decimal(0);
          const orderItemsData: Prisma.OrderItemUncheckedCreateWithoutOrderInput[] = [];

          for (const item of cart.items) {
            if (!item.product.isAvailable) {
              throw new BadRequestException(`Product ${item.product.name} is no longer available`);
            }
            
            const price = new Prisma.Decimal(item.product.price);
            const quantity = new Prisma.Decimal(item.quantity);
            const itemTotal = price.mul(quantity);
            totalAmount = totalAmount.add(itemTotal);

            orderItemsData.push({
              productId: item.productId,
              purchasePrice: price,
              quantity: quantity,
            });
          }

          // 5. Create Order & OrderAddressSnapshot
          const order = await tx.order.create({
            data: {
              userId,
              fulfillmentPointId: fulfillmentPoint.id,
              totalAmount,
              status: 'PENDING',
              items: {
                create: orderItemsData,
              },
              address: {
                create: {
                  street: address.street,
                  city: address.city,
                  state: address.state,
                  postalCode: address.postalCode,
                  country: address.country,
                },
              },
            } as any, // Cast required: Prisma XOR typings fail to infer Without<OrderCreateInput, OrderUncheckedCreateInput> when mixing scalar FKs (userId) and nested relational creates (items.create) in a single payload
            include: {
              items: { include: { product: true } },
              address: true,
            },
          });

          // 6. Clear Cart
          await tx.cartItem.deleteMany({
            where: { cartId: cart.id },
          });

          return order;
        }, {
          isolationLevel: Prisma.TransactionIsolationLevel.Serializable,
        });
      } catch (error) {
        if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2034') {
          attempt++;
          if (attempt >= maxRetries) {
            throw new ConflictException('Order could not be processed due to high concurrency. Please try again later.');
          }
          // Continue to next iteration to retry
          continue;
        }
        // Throw any other errors immediately (like empty cart, unavailable product)
        throw error;
      }
    }
  }

  async findAll(userId: string) {
    return this.prisma.order.findMany({
      where: { userId },
      include: {
        items: { include: { product: true } },
        address: true,
      },
      orderBy: { createdAt: 'desc' }
    });
  }

  async findOne(userId: string, id: string) {
    const order = await this.prisma.order.findUnique({
      where: { id },
      include: {
        items: { include: { product: true } },
        address: true,
      },
    });
    if (!order || order.userId !== userId) {
      throw new NotFoundException('Order not found');
    }
    return order;
  }
}
