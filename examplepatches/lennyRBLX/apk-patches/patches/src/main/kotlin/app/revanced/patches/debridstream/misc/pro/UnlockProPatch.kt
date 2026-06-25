package app.revanced.patches.debridstream.misc.pro
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock pro",
    description = "Unlocks the app.",
) {
    compatibleWith("com.debridstream.tv")

    execute {
        customerInfoFactoryBuildCustomerInfoFingerprint.method.addInstructions(
            0,
            """
                    const-string v3, "subscriber"
                    move-object/from16 v0, p1
                    invoke-virtual {v0, v3}, Lorg/json/JSONObject;->getJSONObject(Ljava/lang/String;)Lorg/json/JSONObject;
                    move-result-object v0
                    
                    const-string v4, "original_app_user_id"
                    invoke-virtual {v0, v4}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v2
                
                    const-string v1, "{\"request_date\":\"2025-11-03T00:30:34Z\",\"request_date_ms\":1762129834506,\"subscriber\":{\"entitlements\":{\"pro\":{\"expires_date\":\"2999-11-06T00:27:17Z\",\"grace_period_expires_date\":null,\"product_identifier\":\"dbs_pro_monthly\",\"product_plan_identifier\":\"monthly-pro-autorenew\",\"purchase_date\":\"2025-11-03T00:27:20Z\"}},\"first_seen\":\"2025-11-03T00:14:44Z\",\"last_seen\":\"2025-11-03T00:14:44Z\",\"management_url\":\"https://play.google.com/store/account/subscriptions\",\"non_subscriptions\":{},\"original_app_user_id\":\"id\",\"original_application_version\":null,\"original_purchase_date\":null,\"other_purchases\":{},\"subscriptions\":{\"dbs_pro_monthly\":{\"auto_resume_date\":null,\"billing_issues_detected_at\":null,\"display_name\":null,\"expires_date\":\"2999-11-06T00:27:17Z\",\"grace_period_expires_date\":null,\"is_sandbox\":false,\"management_url\":\"https://play.google.com/store/account/subscriptions\",\"original_purchase_date\":\"2025-11-03T00:27:20Z\",\"period_type\":\"normal\",\"price\":{\"amount\":0,\"currency\":\"USD\"},\"product_plan_identifier\":\"monthly-pro-autorenew\",\"purchase_date\":\"2025-11-03T00:27:20Z\",\"refunded_at\":null,\"store\":\"play_store\",\"store_transaction_id\":\"GPA.3383-0136-9917-85108\",\"unsubscribe_detected_at\":\"2025-11-03T00:29:11Z\"}}}}"
                    new-instance v0, Lorg/json/JSONObject;
                    invoke-direct {v0, v1}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V
                    
                    invoke-virtual {v0, v3}, Lorg/json/JSONObject;->getJSONObject(Ljava/lang/String;)Lorg/json/JSONObject;
                    move-result-object v1
                    
                    invoke-virtual {v1, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;
                    move-object/from16 p1, v0
            """
        )
    }
}
