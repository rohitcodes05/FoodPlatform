import { Injectable, BadRequestException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { Prisma, ProductType } from '../../generated/prisma/client.js';

@Injectable()
export class CartService {
  constructor(private readonly prisma: PrismaService) {}

  async getCart(userId: string) {
    let cart = await this.prisma.cart.findUnique({
      where: { userId },
      include: {
        items: {
          include: {
            product: {
              select: {
                id: true,
                name: true,
                price: true,
                type: true,
                isAvailable: true,
              },
            },
            cutOption: true,
            weightOption: true,
          },
        },
      },
    });

    if (!cart) {
      cart = await this.prisma.cart.create({
        data: { userId },
        include: { items: { include: { product: true, cutOption: true, weightOption: true } } },
      });
    }

    return cart;
  }

  async addItem(userId: string, productId: string, quantity: number, cutOptionId?: string, weightOptionId?: string) {
    if (quantity <= 0) {
      throw new BadRequestException('Quantity must be positive');
    }

    const product = await this.prisma.product.findUnique({
      where: { id: productId },
      include: { cutOptions: true, weightOptions: true }
    });

    if (!product || !product.isAvailable) {
      throw new NotFoundException('Product not found or unavailable');
    }

    // RAW_MEAT Validation Rule
    if (product.type === ProductType.RAW_MEAT) {
      if (product.cutOptions.length > 0 && !cutOptionId) {
        throw new BadRequestException('A cut option is required for this product');
      }
      if (product.weightOptions.length > 0 && !weightOptionId) {
        throw new BadRequestException('A weight option is required for this product');
      }
    }

    // Validate Variation IDs
    if (cutOptionId) {
      const cut = product.cutOptions.find(c => c.id === cutOptionId);
      if (!cut) throw new BadRequestException('Invalid cut option for this product');
      if (!cut.isAvailable) throw new BadRequestException('Selected cut option is not available');
    }

    if (weightOptionId) {
      const weight = product.weightOptions.find(w => w.id === weightOptionId);
      if (!weight) throw new BadRequestException('Invalid weight option for this product');
      if (!weight.isAvailable) throw new BadRequestException('Selected weight option is not available');
    }

    // Ensure cart exists
    const cart = await this.prisma.cart.upsert({
      where: { userId },
      create: { userId },
      update: {},
    });

    return this.prisma.$transaction(async (tx) => {
      // Find existing cart item with exactly the same variations
      const existingItem = await tx.cartItem.findFirst({
        where: {
          cartId: cart.id,
          productId,
          cutOptionId: cutOptionId || null,
          weightOptionId: weightOptionId || null
        },
      });

      if (existingItem) {
        return tx.cartItem.update({
          where: { id: existingItem.id },
          data: { quantity: new Prisma.Decimal(existingItem.quantity.toNumber() + quantity) },
        });
      } else {
        return tx.cartItem.create({
          data: {
            cartId: cart.id,
            productId,
            cutOptionId: cutOptionId || null,
            weightOptionId: weightOptionId || null,
            quantity: new Prisma.Decimal(quantity),
          },
        });
      }
    });
  }

  async updateItemQuantity(userId: string, cartItemId: string, quantity: number) {
    if (quantity <= 0) {
      throw new BadRequestException('Quantity must be positive');
    }

    const cart = await this.prisma.cart.findUnique({ where: { userId } });
    if (!cart) {
      throw new NotFoundException('Cart not found');
    }

    const cartItem = await this.prisma.cartItem.findUnique({
      where: { id: cartItemId },
      include: { product: true }
    });

    if (!cartItem || cartItem.cartId !== cart.id) {
      throw new NotFoundException('Cart item not found');
    }

    if (!cartItem.product.isAvailable) {
      throw new BadRequestException('Cannot update quantity for an unavailable product');
    }

    return this.prisma.cartItem.update({
      where: { id: cartItemId },
      data: { quantity: new Prisma.Decimal(quantity) },
    });
  }

  async removeItem(userId: string, cartItemId: string) {
    const cart = await this.prisma.cart.findUnique({ where: { userId } });
    if (!cart) {
      throw new NotFoundException('Cart not found');
    }

    const cartItem = await this.prisma.cartItem.findUnique({
      where: { id: cartItemId },
    });

    if (!cartItem || cartItem.cartId !== cart.id) {
      throw new NotFoundException('Cart item not found');
    }

    await this.prisma.cartItem.delete({
      where: { id: cartItemId },
    });
  }

  async clearCart(userId: string) {
    const cart = await this.prisma.cart.findUnique({ where: { userId } });
    if (!cart) return;

    await this.prisma.cartItem.deleteMany({
      where: { cartId: cart.id },
    });
  }
}
