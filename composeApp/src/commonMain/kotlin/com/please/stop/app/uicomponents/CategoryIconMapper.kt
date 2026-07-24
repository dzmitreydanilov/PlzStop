@file:Suppress("MatchingDeclarationName")

package com.please.stop.app.uicomponents

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_bills
import plzstop.composeapp.generated.resources.ic_car
import plzstop.composeapp.generated.resources.ic_charity
import plzstop.composeapp.generated.resources.ic_coffee
import plzstop.composeapp.generated.resources.ic_education
import plzstop.composeapp.generated.resources.ic_entertainment
import plzstop.composeapp.generated.resources.ic_food
import plzstop.composeapp.generated.resources.ic_games
import plzstop.composeapp.generated.resources.ic_gifts
import plzstop.composeapp.generated.resources.ic_groceries
import plzstop.composeapp.generated.resources.ic_gym
import plzstop.composeapp.generated.resources.ic_health
import plzstop.composeapp.generated.resources.ic_housing
import plzstop.composeapp.generated.resources.ic_kids
import plzstop.composeapp.generated.resources.ic_music
import plzstop.composeapp.generated.resources.ic_other
import plzstop.composeapp.generated.resources.ic_pets
import plzstop.composeapp.generated.resources.ic_phone
import plzstop.composeapp.generated.resources.ic_savings
import plzstop.composeapp.generated.resources.ic_shopping
import plzstop.composeapp.generated.resources.ic_subcategory_accessories
import plzstop.composeapp.generated.resources.ic_subcategory_apps
import plzstop.composeapp.generated.resources.ic_subcategory_atm
import plzstop.composeapp.generated.resources.ic_subcategory_bakery
import plzstop.composeapp.generated.resources.ic_subcategory_bank
import plzstop.composeapp.generated.resources.ic_subcategory_bar
import plzstop.composeapp.generated.resources.ic_subcategory_beauty
import plzstop.composeapp.generated.resources.ic_subcategory_bike
import plzstop.composeapp.generated.resources.ic_subcategory_books
import plzstop.composeapp.generated.resources.ic_subcategory_car_repair
import plzstop.composeapp.generated.resources.ic_subcategory_certificate
import plzstop.composeapp.generated.resources.ic_subcategory_chair
import plzstop.composeapp.generated.resources.ic_subcategory_cleaning
import plzstop.composeapp.generated.resources.ic_subcategory_clothing
import plzstop.composeapp.generated.resources.ic_subcategory_cloud
import plzstop.composeapp.generated.resources.ic_subcategory_coffee
import plzstop.composeapp.generated.resources.ic_subcategory_concert
import plzstop.composeapp.generated.resources.ic_subcategory_courses
import plzstop.composeapp.generated.resources.ic_subcategory_dairy
import plzstop.composeapp.generated.resources.ic_subcategory_delivery
import plzstop.composeapp.generated.resources.ic_subcategory_dentist
import plzstop.composeapp.generated.resources.ic_subcategory_doctor
import plzstop.composeapp.generated.resources.ic_subcategory_donations
import plzstop.composeapp.generated.resources.ic_subcategory_education_software
import plzstop.composeapp.generated.resources.ic_subcategory_electronics
import plzstop.composeapp.generated.resources.ic_subcategory_event
import plzstop.composeapp.generated.resources.ic_subcategory_explore
import plzstop.composeapp.generated.resources.ic_subcategory_fastfood
import plzstop.composeapp.generated.resources.ic_subcategory_fees
import plzstop.composeapp.generated.resources.ic_subcategory_frozen
import plzstop.composeapp.generated.resources.ic_subcategory_fuel
import plzstop.composeapp.generated.resources.ic_subcategory_games
import plzstop.composeapp.generated.resources.ic_subcategory_gifts
import plzstop.composeapp.generated.resources.ic_subcategory_gym
import plzstop.composeapp.generated.resources.ic_subcategory_home
import plzstop.composeapp.generated.resources.ic_subcategory_home_goods
import plzstop.composeapp.generated.resources.ic_subcategory_household
import plzstop.composeapp.generated.resources.ic_subcategory_inventory
import plzstop.composeapp.generated.resources.ic_subcategory_kids
import plzstop.composeapp.generated.resources.ic_subcategory_labs
import plzstop.composeapp.generated.resources.ic_subcategory_lunch
import plzstop.composeapp.generated.resources.ic_subcategory_meat
import plzstop.composeapp.generated.resources.ic_subcategory_memberships
import plzstop.composeapp.generated.resources.ic_subcategory_misc
import plzstop.composeapp.generated.resources.ic_subcategory_mobile
import plzstop.composeapp.generated.resources.ic_subcategory_movie
import plzstop.composeapp.generated.resources.ic_subcategory_music
import plzstop.composeapp.generated.resources.ic_subcategory_news
import plzstop.composeapp.generated.resources.ic_subcategory_online_orders
import plzstop.composeapp.generated.resources.ic_subcategory_palette
import plzstop.composeapp.generated.resources.ic_subcategory_pantry
import plzstop.composeapp.generated.resources.ic_subcategory_parking
import plzstop.composeapp.generated.resources.ic_subcategory_pet_food
import plzstop.composeapp.generated.resources.ic_subcategory_pharmacy
import plzstop.composeapp.generated.resources.ic_subcategory_policy
import plzstop.composeapp.generated.resources.ic_subcategory_produce
import plzstop.composeapp.generated.resources.ic_subcategory_repair
import plzstop.composeapp.generated.resources.ic_subcategory_restaurant
import plzstop.composeapp.generated.resources.ic_subcategory_school_supplies
import plzstop.composeapp.generated.resources.ic_subcategory_shoes
import plzstop.composeapp.generated.resources.ic_subcategory_snacks
import plzstop.composeapp.generated.resources.ic_subcategory_sports
import plzstop.composeapp.generated.resources.ic_subcategory_streaming
import plzstop.composeapp.generated.resources.ic_subcategory_subscription_software
import plzstop.composeapp.generated.resources.ic_subcategory_subscription_streaming
import plzstop.composeapp.generated.resources.ic_subcategory_takeout
import plzstop.composeapp.generated.resources.ic_subcategory_taxes
import plzstop.composeapp.generated.resources.ic_subcategory_taxi
import plzstop.composeapp.generated.resources.ic_subcategory_therapy
import plzstop.composeapp.generated.resources.ic_subcategory_toll
import plzstop.composeapp.generated.resources.ic_subcategory_train
import plzstop.composeapp.generated.resources.ic_subcategory_transit
import plzstop.composeapp.generated.resources.ic_subcategory_tuition
import plzstop.composeapp.generated.resources.ic_subcategory_tutoring
import plzstop.composeapp.generated.resources.ic_subcategory_utilities
import plzstop.composeapp.generated.resources.ic_subcategory_vision
import plzstop.composeapp.generated.resources.ic_subcategory_vitamins
import plzstop.composeapp.generated.resources.ic_subcategory_warning
import plzstop.composeapp.generated.resources.ic_subcategory_wifi
import plzstop.composeapp.generated.resources.ic_subcategory_workshops
import plzstop.composeapp.generated.resources.ic_subscriptions
import plzstop.composeapp.generated.resources.ic_tools
import plzstop.composeapp.generated.resources.ic_transport
import plzstop.composeapp.generated.resources.ic_travel

data class CategoryIconOption(val key: String, val drawable: DrawableResource)

val allCategoryIcons: List<CategoryIconOption> = listOf(
    CategoryIconOption("ic_food", Res.drawable.ic_food),
    CategoryIconOption("ic_transport", Res.drawable.ic_transport),
    CategoryIconOption("ic_housing", Res.drawable.ic_housing),
    CategoryIconOption("ic_entertainment", Res.drawable.ic_entertainment),
    CategoryIconOption("ic_groceries", Res.drawable.ic_groceries),
    CategoryIconOption("ic_health", Res.drawable.ic_health),
    CategoryIconOption("ic_shopping", Res.drawable.ic_shopping),
    CategoryIconOption("ic_education", Res.drawable.ic_education),
    CategoryIconOption("ic_subscriptions", Res.drawable.ic_subscriptions),
    CategoryIconOption("ic_coffee", Res.drawable.ic_coffee),
    CategoryIconOption("ic_gym", Res.drawable.ic_gym),
    CategoryIconOption("ic_pets", Res.drawable.ic_pets),
    CategoryIconOption("ic_gifts", Res.drawable.ic_gifts),
    CategoryIconOption("ic_travel", Res.drawable.ic_travel),
    CategoryIconOption("ic_savings", Res.drawable.ic_savings),
    CategoryIconOption("ic_car", Res.drawable.ic_car),
    CategoryIconOption("ic_phone", Res.drawable.ic_phone),
    CategoryIconOption("ic_music", Res.drawable.ic_music),
    CategoryIconOption("ic_games", Res.drawable.ic_games),
    CategoryIconOption("ic_bills", Res.drawable.ic_bills),
    CategoryIconOption("ic_charity", Res.drawable.ic_charity),
    CategoryIconOption("ic_kids", Res.drawable.ic_kids),
    CategoryIconOption("ic_tools", Res.drawable.ic_tools),
    CategoryIconOption("ic_other", Res.drawable.ic_other),
    CategoryIconOption("ic_subcategory_restaurant", Res.drawable.ic_subcategory_restaurant),
    CategoryIconOption("ic_subcategory_coffee", Res.drawable.ic_subcategory_coffee),
    CategoryIconOption("ic_subcategory_delivery", Res.drawable.ic_subcategory_delivery),
    CategoryIconOption("ic_subcategory_fastfood", Res.drawable.ic_subcategory_fastfood),
    CategoryIconOption("ic_subcategory_takeout", Res.drawable.ic_subcategory_takeout),
    CategoryIconOption("ic_subcategory_bakery", Res.drawable.ic_subcategory_bakery),
    CategoryIconOption("ic_subcategory_bar", Res.drawable.ic_subcategory_bar),
    CategoryIconOption("ic_subcategory_lunch", Res.drawable.ic_subcategory_lunch),
    CategoryIconOption("ic_subcategory_fuel", Res.drawable.ic_subcategory_fuel),
    CategoryIconOption("ic_subcategory_transit", Res.drawable.ic_subcategory_transit),
    CategoryIconOption("ic_subcategory_parking", Res.drawable.ic_subcategory_parking),
    CategoryIconOption("ic_subcategory_taxi", Res.drawable.ic_subcategory_taxi),
    CategoryIconOption("ic_subcategory_car_repair", Res.drawable.ic_subcategory_car_repair),
    CategoryIconOption("ic_subcategory_toll", Res.drawable.ic_subcategory_toll),
    CategoryIconOption("ic_subcategory_bike", Res.drawable.ic_subcategory_bike),
    CategoryIconOption("ic_subcategory_train", Res.drawable.ic_subcategory_train),
    CategoryIconOption("ic_subcategory_home", Res.drawable.ic_subcategory_home),
    CategoryIconOption("ic_subcategory_utilities", Res.drawable.ic_subcategory_utilities),
    CategoryIconOption("ic_subcategory_repair", Res.drawable.ic_subcategory_repair),
    CategoryIconOption("ic_subcategory_wifi", Res.drawable.ic_subcategory_wifi),
    CategoryIconOption("ic_subcategory_chair", Res.drawable.ic_subcategory_chair),
    CategoryIconOption("ic_subcategory_inventory", Res.drawable.ic_subcategory_inventory),
    CategoryIconOption("ic_subcategory_policy", Res.drawable.ic_subcategory_policy),
    CategoryIconOption("ic_subcategory_cleaning", Res.drawable.ic_subcategory_cleaning),
    CategoryIconOption("ic_subcategory_movie", Res.drawable.ic_subcategory_movie),
    CategoryIconOption("ic_subcategory_games", Res.drawable.ic_subcategory_games),
    CategoryIconOption("ic_subcategory_streaming", Res.drawable.ic_subcategory_streaming),
    CategoryIconOption("ic_subcategory_concert", Res.drawable.ic_subcategory_concert),
    CategoryIconOption("ic_subcategory_event", Res.drawable.ic_subcategory_event),
    CategoryIconOption("ic_subcategory_palette", Res.drawable.ic_subcategory_palette),
    CategoryIconOption("ic_subcategory_sports", Res.drawable.ic_subcategory_sports),
    CategoryIconOption("ic_subcategory_explore", Res.drawable.ic_subcategory_explore),
    CategoryIconOption("ic_subcategory_produce", Res.drawable.ic_subcategory_produce),
    CategoryIconOption("ic_subcategory_dairy", Res.drawable.ic_subcategory_dairy),
    CategoryIconOption("ic_subcategory_meat", Res.drawable.ic_subcategory_meat),
    CategoryIconOption("ic_subcategory_pantry", Res.drawable.ic_subcategory_pantry),
    CategoryIconOption("ic_subcategory_snacks", Res.drawable.ic_subcategory_snacks),
    CategoryIconOption("ic_subcategory_frozen", Res.drawable.ic_subcategory_frozen),
    CategoryIconOption("ic_subcategory_household", Res.drawable.ic_subcategory_household),
    CategoryIconOption("ic_subcategory_pet_food", Res.drawable.ic_subcategory_pet_food),
    CategoryIconOption("ic_subcategory_pharmacy", Res.drawable.ic_subcategory_pharmacy),
    CategoryIconOption("ic_subcategory_doctor", Res.drawable.ic_subcategory_doctor),
    CategoryIconOption("ic_subcategory_gym", Res.drawable.ic_subcategory_gym),
    CategoryIconOption("ic_subcategory_dentist", Res.drawable.ic_subcategory_dentist),
    CategoryIconOption("ic_subcategory_therapy", Res.drawable.ic_subcategory_therapy),
    CategoryIconOption("ic_subcategory_vitamins", Res.drawable.ic_subcategory_vitamins),
    CategoryIconOption("ic_subcategory_vision", Res.drawable.ic_subcategory_vision),
    CategoryIconOption("ic_subcategory_labs", Res.drawable.ic_subcategory_labs),
    CategoryIconOption("ic_subcategory_clothing", Res.drawable.ic_subcategory_clothing),
    CategoryIconOption("ic_subcategory_electronics", Res.drawable.ic_subcategory_electronics),
    CategoryIconOption("ic_subcategory_shoes", Res.drawable.ic_subcategory_shoes),
    CategoryIconOption("ic_subcategory_accessories", Res.drawable.ic_subcategory_accessories),
    CategoryIconOption("ic_subcategory_beauty", Res.drawable.ic_subcategory_beauty),
    CategoryIconOption("ic_subcategory_home_goods", Res.drawable.ic_subcategory_home_goods),
    CategoryIconOption("ic_subcategory_kids", Res.drawable.ic_subcategory_kids),
    CategoryIconOption("ic_subcategory_online_orders", Res.drawable.ic_subcategory_online_orders),
    CategoryIconOption("ic_subcategory_books", Res.drawable.ic_subcategory_books),
    CategoryIconOption("ic_subcategory_courses", Res.drawable.ic_subcategory_courses),
    CategoryIconOption("ic_subcategory_tuition", Res.drawable.ic_subcategory_tuition),
    CategoryIconOption("ic_subcategory_workshops", Res.drawable.ic_subcategory_workshops),
    CategoryIconOption("ic_subcategory_school_supplies", Res.drawable.ic_subcategory_school_supplies),
    CategoryIconOption("ic_subcategory_certificate", Res.drawable.ic_subcategory_certificate),
    CategoryIconOption("ic_subcategory_tutoring", Res.drawable.ic_subcategory_tutoring),
    CategoryIconOption("ic_subcategory_education_software", Res.drawable.ic_subcategory_education_software),
    CategoryIconOption("ic_subcategory_subscription_software", Res.drawable.ic_subcategory_subscription_software),
    CategoryIconOption("ic_subcategory_subscription_streaming", Res.drawable.ic_subcategory_subscription_streaming),
    CategoryIconOption("ic_subcategory_memberships", Res.drawable.ic_subcategory_memberships),
    CategoryIconOption("ic_subcategory_cloud", Res.drawable.ic_subcategory_cloud),
    CategoryIconOption("ic_subcategory_mobile", Res.drawable.ic_subcategory_mobile),
    CategoryIconOption("ic_subcategory_news", Res.drawable.ic_subcategory_news),
    CategoryIconOption("ic_subcategory_music", Res.drawable.ic_subcategory_music),
    CategoryIconOption("ic_subcategory_apps", Res.drawable.ic_subcategory_apps),
    CategoryIconOption("ic_subcategory_gifts", Res.drawable.ic_subcategory_gifts),
    CategoryIconOption("ic_subcategory_donations", Res.drawable.ic_subcategory_donations),
    CategoryIconOption("ic_subcategory_misc", Res.drawable.ic_subcategory_misc),
    CategoryIconOption("ic_subcategory_fees", Res.drawable.ic_subcategory_fees),
    CategoryIconOption("ic_subcategory_taxes", Res.drawable.ic_subcategory_taxes),
    CategoryIconOption("ic_subcategory_atm", Res.drawable.ic_subcategory_atm),
    CategoryIconOption("ic_subcategory_bank", Res.drawable.ic_subcategory_bank),
    CategoryIconOption("ic_subcategory_warning", Res.drawable.ic_subcategory_warning),
)

private val iconMap: Map<String, DrawableResource> = allCategoryIcons.associate { it.key to it.drawable }

fun categoryIconResourceForKey(iconKey: String): DrawableResource =
    iconMap[iconKey] ?: Res.drawable.ic_other

@Composable
fun CategoryIconImage(
    iconKey: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = vectorResource(categoryIconResourceForKey(iconKey)),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}
