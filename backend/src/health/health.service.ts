import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service.js';

@Injectable()
export class HealthService {
  constructor(private readonly prisma: PrismaService) {}

  async checkHealth() {
    try {
      // Lightweight check to ensure database is reachable
      await this.prisma.$queryRaw`SELECT 1`;
      
      return {
        status: 'ok',
        database: 'ok',
      };
    } catch (error) {
      // Do not expose raw database errors or stack traces
      throw new InternalServerErrorException({
        status: 'error',
        database: 'unreachable',
      });
    }
  }
}
