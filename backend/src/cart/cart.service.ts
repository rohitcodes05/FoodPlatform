import { Injectable, BadRequestException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { Prisma } from '../../generated/prisma/client.js';

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
          },
        },
      },
    });

    if (!cart) {
      cart = await this.prisma.cart.create({
        data: { userId },
        include: { items: { include: { product: true } } },
      });
    }

    return cart;
  }

  async addItem(userId: string, productId: string, quantity: number) {
    if (quantity <= 0) {
      throw new BadRequestException('Quantity must be positive');
    }

    const product = await this.prisma.product.findUnique({ where: { id: productId } });
    if (!product || !product.isAvailable) {
      throw new NotFoundException('Product not found or unavailable');
    }

    // Ensure cart exists
    const cart = await this.prisma.cart.upsert({
      where: { userId },
      create: { userId },
      update: {},
    });

    // We use a Prisma transaction to safely check and update the cart item
    return this.prisma.$transaction(async (tx) => {
      const existingItem = await tx.cartItem.findUnique({
        where: { cartId_productId: { cartId: cart.id, productId } },
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
            quantity: new Prisma.Decimal(quantity),
          },
        });
      }
    });
  }

  async updateItemQuantity(userId: string, productId: string, quantity: number) {
    if (quantity <= 0) {
      throw new BadRequestException('Quantity must be positive');
    }

    const cart = await this.prisma.cart.findUnique({ where: { userId } });
    if (!cart) {
      throw new NotFoundException('Cart not found');
    }

    const product = await this.prisma.product.findUnique({ where: { id: productId } });
    if (!product || !product.isAvailable) {
      throw new BadRequestException('Cannot update quantity for an unavailable product');
    }

    const cartItem = await this.prisma.cartItem.findUnique({
      where: { cartId_productId: { cartId: cart.id, productId } },
    });

    if (!cartItem) {
      throw new NotFoundException('Product not found in cart');
    }

    return this.prisma.cartItem.update({
      where: { id: cartItem.id },
      data: { quantity: new Prisma.Decimal(quantity) },
    });
  }

  async removeItem(userId: string, productId: string) {
    const cart = await this.prisma.cart.findUnique({ where: { userId } });
    if (!cart) {
      throw new NotFoundException('Cart not found');
    }

    const cartItem = await this.prisma.cartItem.findUnique({
      where: { cartId_productId: { cartId: cart.id, productId } },
    });

    if (!cartItem) {
      throw new NotFoundException('Product not found in cart');
    }

    await this.prisma.cartItem.delete({
      where: { id: cartItem.id },
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
