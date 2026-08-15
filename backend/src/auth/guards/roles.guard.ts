import { Injectable, CanActivate, ExecutionContext } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { PrismaService } from '../../prisma/prisma.service';
import { Role } from '../../../generated/prisma/client.js';
import { ROLES_KEY } from '../decorators/roles.decorator';

@Injectable()
export class RolesGuard implements CanActivate {
  constructor(
    private reflector: Reflector,
    private prisma: PrismaService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const requiredRoles = this.reflector.getAllAndOverride<Role[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!requiredRoles) {
      return true;
    }

    const request = context.switchToHttp().getRequest();
    const user = request.user;

    if (!user || !user.id) {
      return false;
    }

    // Database Role Lookup - Ensures authorization is based on CURRENT state, not stale JWT
    const activeRoles = await this.prisma.userRole.findMany({
      where: { userId: user.id },
      select: { role: true }
    });

    const userRoles = activeRoles.map(ur => ur.role);

    return requiredRoles.some((role) => userRoles.includes(role));
  }
}
