package app.revanced.patches.studo.misc

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val hideExpiredProMessagePatch = bytecodePatch(
    name = "Hide Studo-Pro-expired message",
    description = "Hide the message that reminds the user that their StudoPro expired.",
) {
    compatibleWith("com.moshbit.studo"("4.72.2"))

    apply {
        longTextBindPatch.addInstructions(0, """
            invoke-virtual {p1}, Lcom/moshbit/studo/db/OrganizationPostNewsFeedItem;->getPublisherImageUrl()Ljava/lang/String;
            move-result-object v0
            if-eqz v0, :rvbp_show_item

            const-string v1, "https://news.assets.studo.com/cdn-cgi/image/width=120,height=120,fit=crop,format=png/publisherImages/eWqccLSbHMj3R4dXp/666580015"
            invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :rvbp_show_item

            iget-object v0, p0, Landroidx/recyclerview/widget/RecyclerView${'$'}ViewHolder;->itemView:Landroid/view/View;
            const/16 v1, 0x8
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V

            new-instance v1, Landroidx/recyclerview/widget/RecyclerView${'$'}LayoutParams;
            const/4 v2, 0x0
            invoke-direct {v1, v2, v2}, Landroidx/recyclerview/widget/RecyclerView${'$'}LayoutParams;-><init>(II)V
            invoke-virtual {v0, v1}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
            return-void

            :rvbp_show_item
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
