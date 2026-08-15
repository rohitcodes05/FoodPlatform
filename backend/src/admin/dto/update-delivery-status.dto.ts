import { IsEnum, IsString, IsOptional } from 'class-validator';
import { DeliveryStatus } from '../../../generated/prisma/client.js';

export class UpdateDeliveryStatusDto {
  @IsEnum(DeliveryStatus)
  status: DeliveryStatus;

  @IsString()
  @IsOptional()
  trackingCode?: string;
}
