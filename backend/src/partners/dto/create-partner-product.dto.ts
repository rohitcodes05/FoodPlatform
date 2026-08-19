import { IsString, IsNumber, IsOptional, IsBoolean, IsArray, IsEnum, Min, IsNotEmpty } from 'class-validator';
import { ProductType } from '../../../generated/prisma/client.js';

export class CreatePartnerProductDto {
  @IsString()
  @IsNotEmpty()
  name: string;

  @IsString()
  @IsOptional()
  description?: string;

  @IsEnum(ProductType)
  type: ProductType;

  @IsNumber()
  @Min(0)
  price: number;

  @IsBoolean()
  @IsOptional()
  isAvailable?: boolean;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  categoryIds?: string[];
}
