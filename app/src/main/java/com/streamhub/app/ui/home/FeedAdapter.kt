package com.streamhub.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streamhub.app.data.model.ContentItem
import com.streamhub.app.data.model.ContentType
import com.streamhub.app.databinding.ItemFeedCardBinding
import com.streamhub.app.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Feed RecyclerView Adapter with DiffUtil
// ─────────────────────────────────────────────────────────────────────────────

class FeedAdapter(
    private val onItemClick: (ContentItem) -> Unit,
    private val onBookmarkClick: (ContentItem) -> Unit,
    private val onShareClick: (ContentItem) -> Unit,
) : ListAdapter<ContentItem, FeedAdapter.FeedViewHolder>(FeedDiffCallback()) {

    // Track bookmarked IDs externally (from Room Flow)
    private var bookmarkedIds: Set<String> = emptySet()

    fun updateBookmarkedIds(ids: Set<String>) {
        bookmarkedIds = ids
        notifyItemRangeChanged(0, itemCount, BOOKMARK_PAYLOAD)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemFeedCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(getItem(position), bookmarkedIds)
    }

    override fun onBindViewHolder(
        holder: FeedViewHolder, position: Int, payloads: MutableList<Any>
    ) {
        if (payloads.contains(BOOKMARK_PAYLOAD)) {
            holder.updateBookmark(getItem(position).id in bookmarkedIds)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class FeedViewHolder(
        private val binding: ItemFeedCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContentItem, bookmarkedIds: Set<String>) {
            val isBookmarked = item.id in bookmarkedIds

            // Thumbnail
            binding.ivThumbnail.loadUrl(item.thumbnailUrl)

            // Title
            binding.tvTitle.text = item.title

            // Source chip + time
            binding.tvSource.text = item.sourceName
            binding.tvTime.text   = item.publishedAt.toRelativeTime()

            // Description
            binding.tvDescription.text = item.description.takeIf { it.isNotBlank() } ?: ""
            binding.tvDescription.showIf(item.description.isNotBlank())

            // LIVE badge
            binding.chipLive.showIf(item.isLive)

            // Duration badge (only for video/audio)
            val durationText = item.duration?.toDurationString()
            binding.tvDuration.text = durationText ?: ""
            binding.tvDuration.showIf(
                !item.isLive && durationText != null &&
                (item.type == ContentType.VIDEO || item.type == ContentType.AUDIO)
            )

            // View count
            val countText = item.viewCount?.toCompactCount()
            binding.tvViews.text = if (countText != null) "👁 $countText" else ""
            binding.tvViews.showIf(countText != null)

            // Category type chip
            binding.tvType.text = item.type.label

            // Bookmark icon
            updateBookmark(isBookmarked)

            // Click listeners
            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnBookmark.setOnClickListener { onBookmarkClick(item) }
            binding.btnShare.setOnClickListener { onShareClick(item) }
        }

        fun updateBookmark(isBookmarked: Boolean) {
            binding.btnBookmark.setImageResource(
                if (isBookmarked) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            binding.btnBookmark.contentDescription =
                if (isBookmarked) "Remove bookmark" else "Add bookmark"
        }
    }

    companion object {
        private const val BOOKMARK_PAYLOAD = "BOOKMARK"
    }
}

class FeedDiffCallback : DiffUtil.ItemCallback<ContentItem>() {
    override fun areItemsTheSame(old: ContentItem, new: ContentItem) = old.id == new.id
    override fun areContentsTheSame(old: ContentItem, new: ContentItem) = old == new
}
