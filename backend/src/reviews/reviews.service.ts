import { Injectable, NotFoundException, BadRequestException, ConflictException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateReviewDto } from './dto/create-review.dto';
import { UpdateReviewDto } from './dto/update-review.dto';
import { OrderStatus } from '../../generated/prisma/client.js';

@Injectable()
export class ReviewsService {
  constructor(private readonly prisma: PrismaService) {}

  async create(userId: string, createReviewDto: CreateReviewDto) {
    // 1. Fetch OrderItem and its associated Order to verify ownership & eligibility
    const orderItem = await this.prisma.orderItem.findUnique({
      where: { id: createReviewDto.orderItemId },
      include: { order: true, review: true },
    });

    if (!orderItem) {
      throw new NotFoundException('Order item not found');
    }

    if (orderItem.order.userId !== userId) {
      throw new ForbiddenException('You can only review products you have purchased');
    }

    if (orderItem.order.status !== OrderStatus.DELIVERED) {
      throw new BadRequestException('You can only review products after the order has been delivered');
    }

    if (orderItem.review) {
      throw new ConflictException('You have already reviewed this item');
    }

    return this.prisma.review.create({
      data: {
        userId,
        orderItemId: createReviewDto.orderItemId,
        rating: createReviewDto.rating,
        comment: createReviewDto.comment,
      },
    });
  }

  async findAllByProduct(productId: string) {
    // Basic unpaginated return for now; can be paginated easily via query params if needed
    return this.prisma.review.findMany({
      where: {
        orderItem: {
          productId,
        }
      },
      select: {
        id: true,
        rating: true,
        comment: true,
        createdAt: true,
        user: {
          select: {
            name: true,
          }
        }
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  async update(userId: string, id: string, updateReviewDto: UpdateReviewDto) {
    const review = await this.prisma.review.findUnique({
      where: { id },
    });

    if (!review) {
      throw new NotFoundException('Review not found');
    }

    if (review.userId !== userId) {
      throw new ForbiddenException('You can only update your own reviews');
    }

    return this.prisma.review.update({
      where: { id },
      data: updateReviewDto,
    });
  }

  async remove(userId: string, id: string) {
    const review = await this.prisma.review.findUnique({
      where: { id },
    });

    if (!review) {
      throw new NotFoundException('Review not found');
    }

    if (review.userId !== userId) {
      throw new ForbiddenException('You can only delete your own reviews');
    }

    return this.prisma.review.delete({
      where: { id },
    });
  }
}
