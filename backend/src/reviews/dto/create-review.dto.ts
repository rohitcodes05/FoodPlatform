import { IsString, IsInt, Min, Max, IsUUID, IsOptional } from 'class-validator';

export class CreateReviewDto {
  @IsUUID()
  orderItemId: string;

  @IsInt()
  @Min(1)
  @Max(5)
  rating: number;

  @IsOptional()
  @IsString()
  comment?: string;
}
