import { IsString, IsNumber, IsOptional, IsBoolean, IsArray, IsEnum } from 'class-validator';
import { ProductType } from '../../../generated/prisma/client.js';

export class CreateProductDto {
  @IsString()
  name: string;

  @IsString()
  @IsOptional()
  description?: string;

  @IsEnum(ProductType)
  type: ProductType;

  @IsNumber()
  price: number;

  @IsBoolean()
  @IsOptional()
  isAvailable?: boolean;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  categoryIds?: string[];
}
