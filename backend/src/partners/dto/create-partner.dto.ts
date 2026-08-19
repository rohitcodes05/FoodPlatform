import { IsNotEmpty, IsString } from 'class-validator';
import { PartnerType } from '../../../generated/prisma/client.js';

export class CreatePartnerDto {
  @IsString()
  @IsNotEmpty()
  businessName: string;

  @IsString()
  @IsNotEmpty()
  type: PartnerType;
}
