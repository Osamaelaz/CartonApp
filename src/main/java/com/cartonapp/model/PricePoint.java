package com.cartonapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One saved carton price and the date it took effect. */
public record PricePoint(LocalDate date, BigDecimal price) { }
