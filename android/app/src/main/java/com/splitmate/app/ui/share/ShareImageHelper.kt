package com.splitmate.app.ui.share

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * v2.3.5 (#2): writes a rendered PNG into `cacheDir/shared_images/` and opens the Android share
 * sheet through the app's [FileProvider] (authority `${applicationId}.fileprovider`, declared in
 * AndroidManifest.xml with `res/xml/file_paths.xml`). No storage permission is needed.
 */
object ShareImageHelper {

    const val SHARED_IMAGES_DIR = "shared_images"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /** `context.packageName` already carries the `.debug` suffix on debug builds. */
    fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

    /** Deletes previous share images and writes [bitmap] as a fresh, uniquely named PNG. */
    fun writePng(context: Context, bitmap: Bitmap, baseName: String = "splitmate_settle_up"): File {
        val dir = File(context.cacheDir, SHARED_IMAGES_DIR)
        if (dir.exists()) {
            dir.listFiles()?.forEach { runCatching { it.delete() } }
        } else if (!dir.mkdirs() && !dir.exists()) {
            throw java.io.IOException("Cannot create ${dir.absolutePath}")
        }
        val safeBase = baseName.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(48).ifEmpty { "splitmate" }
        val file = File(dir, "${safeBase}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw java.io.IOException("PNG encoding failed")
            }
            out.flush()
        }
        return file
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    /** Opens the system share sheet for a PNG with read permission granted to the receiver. */
    fun shareImage(
        context: Context,
        file: File,
        chooserTitle: String = "Share settle-up",
        caption: String? = null
    ) {
        val uri = uriFor(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (!caption.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Renders [model] on Dispatchers.Default, writes the PNG on Dispatchers.IO, then opens the
     * share sheet on the caller's (main) dispatcher. Returns a failure instead of throwing.
     */
    suspend fun renderAndShare(
        context: Context,
        model: SettleUpShareModel,
        caption: String? = null
    ): Result<Unit> = runCatching {
        val appContext = context.applicationContext
        val file = withContext(Dispatchers.Default) {
            val bitmap = SettleUpImageRenderer.render(model)
            try {
                withContext(Dispatchers.IO) {
                    writePng(appContext, bitmap, "settle_up_${model.tripName}")
                }
            } finally {
                bitmap.recycle()
            }
        }
        shareImage(context, file, chooserTitle = "Share settle-up", caption = caption)
    }

    /** Short plain-text caption sent alongside the image (no UPI IDs / phone numbers). */
    fun captionFor(model: SettleUpShareModel): String =
        if (model.isAllSettled) {
            "${model.tripName}: everyone is settled. Made with SplitMate"
        } else {
            "${model.tripName} settle-up: ${model.transferRows.size + model.hiddenTransferCount} " +
                "${if (model.transferRows.size + model.hiddenTransferCount == 1) "payment" else "payments"}. Made with SplitMate"
        }
}
