package com.streamhub.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.streamhub.app.R
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// View Extensions
// ─────────────────────────────────────────────────────────────────────────────

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

// ─────────────────────────────────────────────────────────────────────────────
// ImageView Extensions
// ─────────────────────────────────────────────────────────────────────────────

fun ImageView.loadUrl(url: String?, placeholder: Int = R.drawable.ic_placeholder) {
    Glide.with(this.context)
        .load(url?.takeIf { it.isNotBlank() })
        .placeholder(placeholder)
        .error(placeholder)
        .centerCrop()
        .transition(DrawableTransitionOptions.withCrossFade(200))
        .into(this)
}

// ─────────────────────────────────────────────────────────────────────────────
// Context Extensions
// ─────────────────────────────────────────────────────────────────────────────

fun Context.toast(msg: String, long: Boolean = false) {
    Toast.makeText(this, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Context.shareUrl(url: String, title: String = "") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n\n$url")
    }
    startActivity(Intent.createChooser(intent, "Share via"))
}

fun Context.openInBrowser(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        toast("No browser found")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Long (timestamp) Extensions
// ─────────────────────────────────────────────────────────────────────────────

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000              -> "Just now"
        diff < 3_600_000           -> "${diff / 60_000}m ago"
        diff < 86_400_000          -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000      -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
    }
}

fun Long.toFormattedDate(): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(this))

// ─────────────────────────────────────────────────────────────────────────────
// Int (duration seconds) Extension
// ─────────────────────────────────────────────────────────────────────────────

fun Int.toDurationString(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}

fun Long.toCompactCount(): String = when {
    this >= 1_000_000 -> "%.1fM".format(this / 1_000_000.0)
    this >= 1_000     -> "%.0fK".format(this / 1_000.0)
    else -> this.toString()
}
