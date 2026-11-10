package com.masroofi.app.data.repository

import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.domain.model.Category

/**
 * دوال تحويل بين كيان جدول التصنيفات ونموذج نطاق العمل النقي
 */
fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        type = type,
        iconKey = iconKey,
        isDefault = isDefault,
        isActive = isActive
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        type = type,
        iconKey = iconKey,
        isDefault = isDefault,
        isActive = isActive
    )
}
