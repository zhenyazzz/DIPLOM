package com.portfolio.domain.api.project.dto;

import java.math.BigDecimal;

public record DepartmentBudgetStatResponse(String departmentCode, BigDecimal totalRequestedBudget) {}
