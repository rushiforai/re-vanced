package app.revanced.patches.studo.pro

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val enableStudoProPatch = bytecodePatch(
    name = "Enable Studo Pro",
    description = "Enable local Studo Pro features: Your Calendar expands to 40 weeks, Exam overview in your calendar, No automated ads, Mail search, View all mail folders, Exam registration notification.",
) {
    compatibleWith("com.moshbit.studo"("4.72.2"))
    // not yet confirmed working: Notification for new grades

    apply {
        settingsIsProPatch.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )

        // To avoid them having any idea about who is using this patch
        // we disable the save-restaurant and the set-calendar-color feature.
        // Both of these would be synchronized to their servers.

        restaurantLikePatch.addInstructions(
            0,
            """
                sget-object p1, Lcom/moshbit/studo/util/DialogManager;->INSTANCE:Lcom/moshbit/studo/util/DialogManager;
                invoke-virtual {p0}, Lcom/moshbit/studo/home/lunch/LunchAdapter;->getFragment()Lcom/moshbit/studo/util/mb/MbFragment;
                move-result-object p0
                invoke-virtual {p0}, Lcom/moshbit/studo/util/mb/MbFragment;->getMbActivity()Lcom/moshbit/studo/util/mb/MbActivity;
                move-result-object p0
                invoke-static {p0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V
                const p2, 0x7f130619
                invoke-virtual {p1, p0, p2}, Lcom/moshbit/studo/util/DialogManager;->showGoProDialog(Lcom/moshbit/studo/util/mb/MbActivity;I)Lcom/afollestad/materialdialogs/MaterialDialog;
                return-void
            """.trimIndent()
        )

        calendarSetColorPatch.addInstructions(
            0,
            """
                sget-object p1, Lcom/moshbit/studo/util/DialogManager;->INSTANCE:Lcom/moshbit/studo/util/DialogManager;
                invoke-virtual {p0}, Lcom/moshbit/studo/util/mb/MbFragment;->getMbActivity()Lcom/moshbit/studo/util/mb/MbActivity;
                move-result-object p0
                invoke-static {p0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V
                const/4 v0, 0x2
                const/4 v1, 0x0
                const/4 v2, 0x0
                invoke-static {p1, p0, v2, v0, v1}, Lcom/moshbit/studo/util/DialogManager;->showGoProDialog${'$'}default(Lcom/moshbit/studo/util/DialogManager;Lcom/moshbit/studo/util/mb/MbActivity;IILjava/lang/Object;)Lcom/afollestad/materialdialogs/MaterialDialog;
                return-void
            """.trimIndent()
        )
    }
}
