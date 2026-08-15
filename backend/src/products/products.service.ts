import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { GetProductsFilterDto } from './dto/get-products-filter.dto';
import { Prisma } from '../../generated/prisma/client.js';

@Injectable()
export class ProductsService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(filterDto: GetProductsFilterDto) {
    const { page = 1, limit = 20, categoryId, type, search } = filterDto;
    const skip = (page - 1) * limit;

    const where: Prisma.ProductWhereInput = {
      isAvailable: true, // Only show available products to customers
    };

    if (categoryId) {
      where.categories = {
        some: { id: categoryId },
      };
    }

    if (type) {
      where.type = type;
    }

    if (search) {
      where.name = {
        contains: search,
        mode: 'insensitive',
      };
    }

    const [items, total] = await Promise.all([
      this.prisma.product.findMany({
        where,
        skip,
        take: limit,
        include: {
          categories: {
            select: { id: true, name: true }
          }
        },
        orderBy: { createdAt: 'desc' },
      }),
      this.prisma.product.count({ where }),
    ]);

    return {
      items,
      meta: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  async findOne(id: string) {
    const product = await this.prisma.product.findUnique({
      where: { id },
      include: {
        categories: {
          select: { id: true, name: true }
        }
      }
    });

    if (!product || !product.isAvailable) {
      throw new NotFoundException('Product not found or unavailable');
    }

    return product;
  }
}
