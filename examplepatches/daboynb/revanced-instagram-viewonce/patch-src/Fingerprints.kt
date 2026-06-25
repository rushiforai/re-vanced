package app.revanced.patches.instagram.direct.viewonce

import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.patch.BytecodePatchContext

/**
 * Finds the view-once media error callback class via the "photo_cant_load" string.
 * The success callback is another method in the same class that references
 * DirectVisualMessageViewerController.
 */
internal val BytecodePatchContext.photoErrorCallbackMethod
    by gettingFirstMethodDeclaratively("photo_cant_load")

