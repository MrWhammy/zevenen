package com.zevenen.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

/** Writes the score image to the cache dir and hands it to the system share sheet. */
fun shareScores(context: Context, image: Bitmap, text: String) {
    val dir = File(context.cacheDir, "shares").apply { mkdirs() }
    val file = File(dir, "zevenen-final-scores.png")
    file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share final scores"))
}
