package com.joi.designsystem.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Renders a QR code from the raw PNG bytes the backend returns (`GET /users/:id/qr`).
 * No image-loading library needed — the bytes already ARE the whole image, so this just
 * decodes them once and draws them; deliberately doesn't touch the network itself, so this
 * module never needs to depend on `data`.
 */
@Composable
fun QrCodeImage(pngBytes: ByteArray, modifier: Modifier = Modifier) {
    val bitmap = remember(pngBytes) {
        BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size).asImageBitmap()
    }
    Surface(
        modifier = modifier.size(220.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "QR code",
            contentScale = ContentScale.Fit,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).size(220.dp),
        )
    }
}
