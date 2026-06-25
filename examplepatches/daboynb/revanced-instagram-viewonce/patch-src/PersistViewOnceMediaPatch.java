package app.revanced.extension.instagram.direct.viewonce;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.reflect.Field;

/**
 * Extension injected into the view-once media success callback.
 * Navigates: callback → controller → contentHolder → ImageView → save bitmap.
 * For videos: finds VideoUrlImpl → extracts URL → downloads.
 */
public final class PersistViewOnceMediaPatch {

    private static final String TAG = "RV-ViewOnce";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static long lastSaveTimestamp = 0;

    /**
     * Called from the patched success callback method.
     * @param callbackInstance the callback object (has fields pointing to the controller)
     * @param mediaParam the media parameter passed to the success callback
     */
    public static void onViewOnceMediaLoaded(Object callbackInstance, Object mediaParam) {
        // Deduplicate: ignore callbacks within 3 seconds of last save
        long now = System.currentTimeMillis();
        if (now - lastSaveTimestamp < 3000) {
            return;
        }
        lastSaveTimestamp = now;

        Log.i(TAG, "onViewOnceMediaLoaded: callback fired, class=" + callbackInstance.getClass().getName());

        // Post delayed to ensure the bitmap/video is fully rendered
        handler.postDelayed(() -> {
            try {
                Object controller = findController(callbackInstance);
                if (controller == null) {
                    Log.e(TAG, "Controller not found");
                    return;
                }
                Log.i(TAG, "Found controller: " + controller.getClass().getName());

                // Check if this is a video (controller has non-null videoPlayer field)
                boolean isVideo = false;
                try {
                    Field vpField = controller.getClass().getDeclaredField("videoPlayer");
                    vpField.setAccessible(true);
                    Object vp = vpField.get(controller);
                    isVideo = vp != null;
                    Log.d(TAG, "videoPlayer field found, isNull=" + (vp == null));
                } catch (NoSuchFieldException e) {
                    // Try superclass
                    try {
                        Field vpField = controller.getClass().getSuperclass().getDeclaredField("videoPlayer");
                        vpField.setAccessible(true);
                        Object vp = vpField.get(controller);
                        isVideo = vp != null;
                        Log.d(TAG, "videoPlayer in superclass, isNull=" + (vp == null));
                    } catch (Exception e2) {
                        // Dump all field names for diagnostics
                        StringBuilder sb = new StringBuilder("Controller fields: ");
                        for (Field f : controller.getClass().getDeclaredFields()) {
                            sb.append(f.getName()).append("(").append(f.getType().getSimpleName()).append(") ");
                        }
                        Log.d(TAG, sb.toString());
                    }
                } catch (Exception e) {
                    Log.w(TAG, "videoPlayer check: " + e.getMessage());
                }

                if (isVideo) {
                    Log.i(TAG, "Video detected, searching for URL...");
                    // Search in: videoPlayer, callback fields, controller fields
                    String videoUrl = extractVideoUrlFromPlayer(controller);
                    if (videoUrl == null) {
                        videoUrl = findVideoUrlImpl(callbackInstance, 0);
                    }
                    if (videoUrl != null) {
                        Log.i(TAG, "Found video URL, downloading...");
                        downloadVideo(videoUrl);
                    } else {
                        Log.w(TAG, "Could not find video URL, saving thumbnail");
                        trySavePhoto(controller);
                    }
                } else {
                    trySavePhoto(controller);
                }
            } catch (Exception e) {
                Log.e(TAG, "onViewOnceMediaLoaded error: " + e.getMessage());
            }
        }, 1500);
    }

    /**
     * Navigate from callback to DirectVisualMessageViewerController.
     * Searches up to 2 levels deep in the object graph.
     */
    private static Object findController(Object callback) {
        String controllerName = "com.instagram.direct.visual.DirectVisualMessageViewerController";
        try {
            // Level 1: check direct fields
            for (Field f : callback.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(callback);
                if (val == null) continue;

                if (val.getClass().getName().equals(controllerName)) {
                    return val;
                }

                // Level 2: check fields of fields
                for (Field f2 : val.getClass().getDeclaredFields()) {
                    try {
                        f2.setAccessible(true);
                        if (f2.getType().getName().equals(controllerName)) {
                            Object controller = f2.get(val);
                            if (controller != null) return controller;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "findController error: " + e.getMessage());
        }
        return null;
    }

    private static boolean trySavePhoto(Object controller) {
        try {
            // controller.contentHolder (non-obfuscated field)
            Field contentHolderField = controller.getClass().getDeclaredField("contentHolder");
            contentHolderField.setAccessible(true);
            Object contentHolder = contentHolderField.get(controller);
            if (contentHolder == null) {
                Log.w(TAG, "contentHolder is null");
                return false;
            }

            // Find IgProgressImageView in contentHolder
            Object progressImageView = findFieldByTypeName(contentHolder, "IgProgressImageView");
            if (progressImageView == null) {
                // Fallback: find any ImageView
                progressImageView = findFieldByTypeName(contentHolder, "ImageView");
            }
            if (progressImageView == null) {
                Log.w(TAG, "No ImageView found in contentHolder");
                return false;
            }

            // Find the actual ImageView (might be nested)
            ImageView imageView = null;
            if (progressImageView instanceof ImageView) {
                imageView = (ImageView) progressImageView;
            } else {
                // Try to find IgImageView inside
                Object igImageView = findFieldByTypeName(progressImageView, "IgImageView");
                if (igImageView == null) {
                    igImageView = findFieldByTypeName(progressImageView, "ImageView");
                }
                if (igImageView instanceof ImageView) {
                    imageView = (ImageView) igImageView;
                }
            }

            if (imageView == null) {
                Log.w(TAG, "Could not resolve ImageView");
                return false;
            }

            Drawable drawable = imageView.getDrawable();
            if (drawable == null) {
                Log.w(TAG, "Drawable is null");
                return false;
            }

            Log.i(TAG, "ImageView: " + imageView.getWidth() + "x" + imageView.getHeight()
                    + " drawable=" + drawable.getClass().getName());

            Bitmap bitmap = null;

            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            }

            if (bitmap == null) {
                int w = imageView.getWidth();
                int h = imageView.getHeight();
                if (w > 0 && h > 0) {
                    bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    imageView.draw(canvas);
                }
            }

            if (bitmap != null && !bitmap.isRecycled()) {
                saveBitmap(bitmap);
                return true;
            }
        } catch (NoSuchFieldException e) {
            Log.w(TAG, "contentHolder field not found: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "trySavePhoto error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Extract video URL by navigating: controller.videoPlayer -> fields -> VideoUrlImpl -> URL string.
     * VideoUrlImpl is a non-obfuscated class with a String field containing the CDN URL.
     */
    private static String extractVideoUrlFromPlayer(Object controller) {
        try {
            Field vpField = controller.getClass().getDeclaredField("videoPlayer");
            vpField.setAccessible(true);
            Object videoPlayer = vpField.get(controller);
            if (videoPlayer == null) return null;

            Log.d(TAG, "videoPlayer class: " + videoPlayer.getClass().getName());

            // Search recursively in the videoPlayer for VideoUrlImpl instances
            return findVideoUrlImpl(videoPlayer, 0);
        } catch (Exception e) {
            Log.w(TAG, "extractVideoUrlFromPlayer: " + e.getMessage());
        }
        return null;
    }

    /**
     * Recursively search an object's fields for a VideoUrlImpl instance.
     * VideoUrlImpl has a non-obfuscated class name and a String URL field.
     */
    private static String findVideoUrlImpl(Object obj, int depth) {
        if (obj == null || depth > 4) return null;
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val == null) continue;

                String className = val.getClass().getName();

                // Direct match: VideoUrlImpl
                if (className.equals("com.instagram.model.mediasize.VideoUrlImpl")) {
                    // Extract URL from VideoUrlImpl (stored in field A06 or first non-null String)
                    for (Field urlField : val.getClass().getDeclaredFields()) {
                        if (urlField.getType() == String.class) {
                            urlField.setAccessible(true);
                            String url = (String) urlField.get(val);
                            if (url != null && url.startsWith("http")) {
                                Log.d(TAG, "Found VideoUrlImpl URL in " + f.getName());
                                return url;
                            }
                        }
                    }
                }

                // Check if it's a List that might contain VideoUrlImpl
                if (val instanceof java.util.List) {
                    for (Object item : (java.util.List<?>) val) {
                        if (item != null && item.getClass().getName().contains("VideoUrlImpl")) {
                            for (Field urlField : item.getClass().getDeclaredFields()) {
                                if (urlField.getType() == String.class) {
                                    urlField.setAccessible(true);
                                    String url = (String) urlField.get(item);
                                    if (url != null && url.startsWith("http")) {
                                        Log.d(TAG, "Found VideoUrlImpl URL in list " + f.getName());
                                        return url;
                                    }
                                }
                            }
                        }
                    }
                }

                // Recurse into non-primitive objects (skip common types)
                if (depth < 3 && !className.startsWith("java.") && !className.startsWith("android.")
                        && !val.getClass().isPrimitive() && !val.getClass().isEnum()
                        && !(val instanceof String) && !(val instanceof Number)) {
                    String url = findVideoUrlImpl(val, depth + 1);
                    if (url != null) return url;
                }
            }
        } catch (Exception e) {
            if (depth == 0) Log.w(TAG, "findVideoUrlImpl: " + e.getMessage());
        }
        return null;
    }

    private static void downloadVideo(final String videoUrl) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(videoUrl).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                if (conn.getResponseCode() != 200) {
                    Log.e(TAG, "Video download failed: HTTP " + conn.getResponseCode());
                    return;
                }
                String contentType = conn.getContentType();
                Log.d(TAG, "Video download: content-type=" + contentType + " length=" + conn.getContentLength());
                File dir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "InstagramViewOnce");
                if (!dir.exists()) dir.mkdirs();
                String filename = "viewonce_" + System.currentTimeMillis() + ".mp4";
                File file = new File(dir, filename);
                InputStream is = conn.getInputStream();
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    try {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                        Log.i(TAG, "Video saved: " + file.getAbsolutePath()
                                + " (" + file.length() / 1024 + "KB)");
                    } finally {
                        fos.close();
                    }
                } finally {
                    is.close();
                    conn.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "downloadVideo error: " + e.getMessage());
            }
        }).start();
    }

    private static Object findFieldByTypeName(Object obj, String typeNamePart) {
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains(typeNamePart)) {
                    f.setAccessible(true);
                    return f.get(obj);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "findFieldByTypeName(" + typeNamePart + ") error: " + e.getMessage());
        }
        return null;
    }

    private static void saveBitmap(Bitmap bitmap) {
        try {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "InstagramViewOnce");
            if (!dir.exists()) dir.mkdirs();
            String filename = "viewonce_" + System.currentTimeMillis() + ".webp";
            File file = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            try {
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, fos);
                fos.flush();
                Log.i(TAG, "Photo saved: " + file.getAbsolutePath());
            } finally {
                fos.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "saveBitmap error: " + e.getMessage());
        }
    }

}
