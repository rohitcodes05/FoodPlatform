import { Controller, Post, Get, Body, UseGuards } from '@nestjs/common';
import { PartnersService } from './partners.service';
import { CreatePartnerDto } from './dto/create-partner.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { CurrentUser } from '../auth/current-user.decorator';
import type { User } from '../../generated/prisma/client.js';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { Role } from '../../generated/prisma/client.js';

@Controller('partners')
@UseGuards(JwtAuthGuard)
export class PartnersController {
  constructor(private readonly partnersService: PartnersService) {}

  @Post()
  createPartner(@CurrentUser() user: User, @Body() createPartnerDto: CreatePartnerDto) {
    return this.partnersService.createPartner(user.id, createPartnerDto);
  }

  @Get('me')
  @UseGuards(RolesGuard)
  @Roles(Role.VENDOR)
  getMyPartnerProfile(@CurrentUser() user: User) {
    return this.partnersService.getMyPartnerProfile(user.id);
  }
}
