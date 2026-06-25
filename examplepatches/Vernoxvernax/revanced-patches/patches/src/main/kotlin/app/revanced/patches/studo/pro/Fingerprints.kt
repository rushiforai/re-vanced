package app.revanced.patches.studo.pro

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.settingsIsProPatch by gettingFirstMethodDeclaratively {
    definingClass("Lcom/moshbit/studo/app/Settings;")
    name("isPro")
    returnType("Z")
    parameterTypes()
}

internal val BytecodePatchContext.restaurantLikePatch by gettingFirstMethodDeclaratively {
    definingClass("Lcom/moshbit/studo/home/lunch/LunchAdapter\$RestaurantVH;")
    name($$"_init_$lambda$2")
    returnType("V")
    parameterTypes(
        "Lcom/moshbit/studo/home/lunch/LunchAdapter;",
        $$"Lcom/moshbit/studo/home/lunch/LunchAdapter$RestaurantVH;",
        "Landroid/view/View;",
    )
}

internal val BytecodePatchContext.calendarSetColorPatch by gettingFirstMethodDeclaratively {
    definingClass("Lcom/moshbit/studo/home/calendar/CalendarAddFragment;")
    name($$"onViewLazilyCreated$lambda$35")
    returnType("V")
    parameterTypes(
        "Lcom/moshbit/studo/home/calendar/CalendarAddFragment;",
        "Landroid/view/View;",
    )
}
