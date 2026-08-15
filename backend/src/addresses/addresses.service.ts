import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateAddressDto } from './dto/create-address.dto';
import { UpdateAddressDto } from './dto/update-address.dto';

@Injectable()
export class AddressesService {
  constructor(private readonly prisma: PrismaService) {}

  async create(userId: string, createAddressDto: CreateAddressDto) {
    return this.prisma.address.create({
      data: {
        userId,
        ...createAddressDto,
      },
    });
  }

  async findAll(userId: string) {
    return this.prisma.address.findMany({
      where: { userId },
    });
  }

  async findOne(userId: string, id: string) {
    const address = await this.prisma.address.findUnique({
      where: { id },
    });
    if (!address || address.userId !== userId) {
      throw new NotFoundException('Address not found');
    }
    return address;
  }

  async update(userId: string, id: string, updateAddressDto: UpdateAddressDto) {
    const address = await this.findOne(userId, id); // Ensure it belongs to user
    return this.prisma.address.update({
      where: { id: address.id },
      data: updateAddressDto,
    });
  }

  async remove(userId: string, id: string) {
    const address = await this.findOne(userId, id); // Ensure it belongs to user
    return this.prisma.address.delete({
      where: { id: address.id },
    });
  }
}
