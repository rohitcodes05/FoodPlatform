import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { GetProductsFilterDto } from './dto/get-products-filter.dto';
import { Prisma, ProductType } from '../../generated/prisma/client.js';

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

  async createProduct(data: { name: string; description?: string; type: ProductType; price: number; isAvailable?: boolean; categoryIds?: string[] }) {
    return this.prisma.product.create({
      data: {
        name: data.name,
        description: data.description,
        type: data.type,
        price: data.price,
        isAvailable: data.isAvailable ?? true,
        categories: {
          connect: data.categoryIds?.map(id => ({ id })) || []
        }
      },
      include: { categories: true }
    });
  }

  async updateProduct(id: string, data: { name?: string; description?: string; type?: ProductType; price?: number; isAvailable?: boolean; categoryIds?: string[] }) {
    return this.prisma.product.update({
      where: { id },
      data: {
        name: data.name,
        description: data.description,
        type: data.type,
        price: data.price,
        isAvailable: data.isAvailable,
        ...(data.categoryIds && {
          categories: {
            set: data.categoryIds.map(catId => ({ id: catId }))
          }
        })
      },
      include: { categories: true }
    });
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
