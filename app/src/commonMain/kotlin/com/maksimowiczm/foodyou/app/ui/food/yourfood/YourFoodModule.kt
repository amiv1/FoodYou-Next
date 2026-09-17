package com.maksimowiczm.foodyou.app.ui.food.yourfood

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

fun Module.yourFood() {
    viewModelOf(::YourFoodViewModel)
}
