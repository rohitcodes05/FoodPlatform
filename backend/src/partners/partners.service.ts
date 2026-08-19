import { Injectable, ConflictException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreatePartnerDto } from './dto/create-partner.dto';
import { Role } from '../../generated/prisma/client.js';

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
}
