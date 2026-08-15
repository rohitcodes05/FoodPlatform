import { Controller, Get, Param, Query } from '@nestjs/common';
import { ProductsService } from './products.service';
import { GetProductsFilterDto } from './dto/get-products-filter.dto';

@Controller('products')
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  @Get()
  findAll(@Query() filterDto: GetProductsFilterDto) {
    return this.productsService.findAll(filterDto);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.productsService.findOne(id);
  }
}
