package app.revanced.patches.studo.misc

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val hidePromotionalNewsPatch = bytecodePatch(
    name = "Hide Promotional News",
    description = "Hides unrelated news, allowing only Chat topics and Job listings.",
) {
    compatibleWith("com.moshbit.studo"("4.72.2"))

    apply {
        longTextBindWhitelistPatch.addInstructions(0, """
            invoke-virtual {p1}, Lcom/moshbit/studo/db/OrganizationPostNewsFeedItem;->getContentText()Ljava/lang/String;
            move-result-object v0
            if-eqz v0, :rvwt_hide_item

            const-string v1, "Chat"
            invoke-virtual {v0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
            move-result v0
            if-nez v0, :rvwt_show_item

            :rvwt_hide_item
            iget-object v0, p0, Landroidx/recyclerview/widget/RecyclerView${'$'}ViewHolder;->itemView:Landroid/view/View;
            const/16 v1, 0x8
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V

            new-instance v1, Landroidx/recyclerview/widget/RecyclerView${'$'}LayoutParams;
            const/4 v2, 0x0
            invoke-direct {v1, v2, v2}, Landroidx/recyclerview/widget/RecyclerView${'$'}LayoutParams;-><init>(II)V
            invoke-virtual {v0, v1}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
            return-void

            :rvwt_show_item
            iget-object v0, p0, Landroidx/recyclerview/widget/RecyclerView${'$'}ViewHolder;->itemView:Landroid/view/View;
            const/4 v1, 0x0
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V

            new-instance v1, Landroidx/recyclerview/widget/RecyclerView${'$'}LayoutParams;
            const/4 v2, -0x2
            invoke-direct {v1, v2, v2}, Landroidx/recyclerview/widget/RecyclerView${'$'}LayoutParams;-><init>(II)V
            invoke-virtual {v0, v1}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
        """.trimIndent()
        )
    }
}
