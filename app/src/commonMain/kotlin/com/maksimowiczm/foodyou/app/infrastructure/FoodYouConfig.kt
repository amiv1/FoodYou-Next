package com.maksimowiczm.foodyou.app.infrastructure

import com.maksimowiczm.foodyou.app.BuildConfig
import com.maksimowiczm.foodyou.common.config.AppConfig
import com.maksimowiczm.foodyou.common.config.NetworkConfig

internal class FoodYouConfig : AppConfig, NetworkConfig {
    override val versionName: String = BuildConfig.VERSION_NAME
    override val translationUri: String = "https://crowdin.com/project/food-you"
    override val sourceCodeUri: String = "https://github.com/amiv1/ForkLog"
    override val issueTrackerUri: String = "https://github.com/amiv1/ForkLog/issues"
    override val privacyPolicyUri: String = "https://amiv1.github.io/foodtracker/privacy.html"
    override val openFoodFactsTermsOfUseUri: String = "https://world.openfoodfacts.org/terms-of-use"
    override val openFoodFactsPrivacyPolicyUri: String = "https://world.openfoodfacts.org/privacy"
    override val foodDataCentralPrivacyPolicyUri: String = "https://www.usda.gov/privacy-policy"

    override val userAgent: String = "Fork Log/$versionName"
}
