package com.nex.clone.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/** Menyimpan ikon kustom klon (dipangkas persegi 192px) di penyimpanan internal. */
object IconUtils {
    private fun dir(ctx: Context) = File(ctx.filesDir, "icons").apply { mkdirs() }

    fun saveFromUri(ctx: Context, pkg: String, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val s = minOf(bmp.width, bmp.height)
            val square = Bitmap.createBitmap(bmp, (bmp.width - s) / 2, (bmp.height - s) / 2, s, s)
            val out = Bitmap.createScaledBitmap(square, 192, 192, true)
            val f = File(dir(ctx), "$pkg.png")
            FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            f.name
        } catch (e: Exception) {
            null
        }
    }

    fun loadCustom(ctx: Context, fileName: String?): Bitmap? {
        if (fileName == null) return null
        return runCatching { BitmapFactory.decodeFile(File(dir(ctx), fileName).absolutePath) }.getOrNull()
    }

    fun delete(ctx: Context, fileName: String?) {
        if (fileName != null) runCatching { File(dir(ctx), fileName).delete() }
    }
}
