import { IsString, IsNumber, IsOptional, IsBoolean, IsArray, IsEnum, Min } from 'class-validator';
import { ProductType } from '../../../generated/prisma/client.js';

export class UpdatePartnerProductDto {
  @IsString()
  @IsOptional()
  name?: string;

  @IsString()
  @IsOptional()
  description?: string;

  @IsEnum(ProductType)
  @IsOptional()
  type?: ProductType;

  @IsNumber()
  @Min(0)
  @IsOptional()
  price?: number;

  @IsBoolean()
  @IsOptional()
  isAvailable?: boolean;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  categoryIds?: string[];
}
