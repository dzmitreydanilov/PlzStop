package com.please.stop.app.uicomponents

fun tagsForCategoryKey(iconKey: String): List<String> = when (iconKey) {
    "ic_food" -> listOf("Lunch", "Dinner", "Coffee", "Snack", "Takeout", "Breakfast")
    "ic_transport" -> listOf("Uber", "Gas", "Parking", "Bus", "Metro", "Toll")
    "ic_housing" -> listOf("Rent", "Utilities", "Repairs", "Internet", "Insurance", "Cleaning")
    "ic_entertainment" -> listOf("Movies", "Games", "Concerts", "Streaming", "Sports", "Hobbies")
    "ic_groceries" -> listOf("Weekly", "Fruits", "Drinks", "Snacks", "Household", "Organic")
    "ic_health" -> listOf("Pharmacy", "Doctor", "Gym", "Dental", "Vitamins", "Therapy")
    "ic_shopping" -> listOf("Clothes", "Electronics", "Gifts", "Home", "Beauty", "Accessories")
    "ic_education" -> listOf("Books", "Course", "Tutor", "Supplies", "Software", "Exam")
    "ic_subscriptions" -> listOf("Music", "Cloud", "News", "App", "Membership", "Premium")
    "ic_other" -> listOf("Cash", "Transfer", "Fee", "Tip", "Donation", "Misc")
    else -> listOf("General", "Quick", "Planned", "Urgent", "One-time")
}
