import { IsBoolean, IsNotEmpty, IsNumber, IsOptional, IsString, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class CreateCutOptionDto {
  @IsString()
  @IsNotEmpty()
  name!: string;

  @IsBoolean()
  @IsOptional()
  isAvailable?: boolean;
}

export class UpdateCutOptionDto {
  @IsString()
  @IsOptional()
  name?: string;

  @IsBoolean()
  @IsOptional()
  isAvailable?: boolean;
}

export class CreateWeightOptionDto {
  @IsString()
  @IsNotEmpty()
  weightLabel!: string;

  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  @Type(() => Number)
  priceOverride!: number;

  @IsBoolean()
  @IsOptional()
  isAvailable?: boolean;
}

export class UpdateWeightOptionDto {
  @IsString()
  @IsOptional()
  weightLabel?: string;

  @IsNumber({ maxDecimalPlaces: 2 })
  @Min(0)
  @IsOptional()
  @Type(() => Number)
  priceOverride?: number;

  @IsBoolean()
  @IsOptional()
  isAvailable?: boolean;
}
