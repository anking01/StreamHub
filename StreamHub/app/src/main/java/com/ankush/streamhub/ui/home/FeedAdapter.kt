package com.ankush.streamhub.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ankush.streamhub.R
import com.ankush.streamhub.ai.SummaryState
import com.ankush.streamhub.data.model.ContentItem
import com.ankush.streamhub.data.model.ContentType
import com.ankush.streamhub.databinding.ItemFeedCardBinding
import com.ankush.streamhub.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Feed RecyclerView Adapter with DiffUtil
// ─────────────────────────────────────────────────────────────────────────────

class FeedAdapter(
    private val onItemClick: (ContentItem) -> Unit,
    private val onBookmarkClick: (ContentItem) -> Unit,
    private val onShareClick: (ContentItem) -> Unit,
    private val onSummarizeClick: (ContentItem) -> Unit,
) : ListAdapter<ContentItem, FeedAdapter.FeedViewHolder>(FeedDiffCallback()) {

    private var bookmarkedIds: Set<String> = emptySet()
    private var summaryStates: Map<String, SummaryState> = emptyMap()

    fun updateBookmarkedIds(ids: Set<String>) {
        bookmarkedIds = ids
        notifyItemRangeChanged(0, itemCount, BOOKMARK_PAYLOAD)
    }

    fun updateSummaryStates(states: Map<String, SummaryState>) {
        summaryStates = states
        notifyItemRangeChanged(0, itemCount, SUMMARY_PAYLOAD)
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
        when {
            payloads.contains(BOOKMARK_PAYLOAD) ->
                holder.updateBookmark(getItem(position).id in bookmarkedIds)
            payloads.contains(SUMMARY_PAYLOAD) ->
                holder.updateSummary(summaryStates[getItem(position).id] ?: SummaryState.Idle)
            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class FeedViewHolder(
        private val binding: ItemFeedCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContentItem, bookmarkedIds: Set<String>) {
            binding.ivThumbnail.loadUrl(item.thumbnailUrl)
            binding.tvTitle.text = item.title
            binding.tvSource.text = item.sourceName
            binding.tvTime.text = item.publishedAt.toRelativeTime()

            binding.tvDescription.text = item.description.takeIf { it.isNotBlank() } ?: ""
            binding.tvDescription.showIf(item.description.isNotBlank())

            binding.chipLive.showIf(item.isLive)

            val durationText = item.duration?.toDurationString()
            binding.tvDuration.text = durationText ?: ""
            binding.tvDuration.showIf(
                !item.isLive && durationText != null &&
                (item.type == ContentType.VIDEO || item.type == ContentType.AUDIO)
            )

            val countText = item.viewCount?.toCompactCount()
            binding.tvViews.text = if (countText != null) "👁 $countText" else ""
            binding.tvViews.showIf(countText != null)

            binding.tvType.text = item.type.label

            updateBookmark(item.id in bookmarkedIds)
            updateSummary(summaryStates[item.id] ?: SummaryState.Idle)

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnBookmark.setOnClickListener { onBookmarkClick(item) }
            binding.btnShare.setOnClickListener { onShareClick(item) }
            binding.btnTldr.setOnClickListener { onSummarizeClick(item) }
        }

        fun updateBookmark(isBookmarked: Boolean) {
            binding.btnBookmark.setImageResource(
                if (isBookmarked) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            binding.btnBookmark.contentDescription =
                if (isBookmarked) "Remove bookmark" else "Add bookmark"
        }

        fun updateSummary(state: SummaryState) {
            val ctx = binding.root.context
            when (state) {
                is SummaryState.Idle -> {
                    binding.layoutSummaryContent.visibility = View.GONE
                    binding.btnTldr.text = "✨ TL;DR"
                    binding.btnTldr.isEnabled = true
                }
                is SummaryState.Loading -> {
                    binding.layoutSummaryContent.visibility = View.VISIBLE
                    binding.progressSummary.visibility = View.VISIBLE
                    binding.tvSummary.visibility = View.GONE
                    binding.btnTldr.text = "Summarizing…"
                    binding.btnTldr.isEnabled = false
                }
                is SummaryState.Success -> {
                    binding.layoutSummaryContent.visibility = View.VISIBLE
                    binding.progressSummary.visibility = View.GONE
                    binding.tvSummary.visibility = View.VISIBLE
                    binding.tvSummary.text = state.bullets
                    binding.tvSummary.setTextColor(
                        ContextCompat.getColor(ctx, R.color.text_secondary)
                    )
                    binding.btnTldr.text = "✨ TL;DR"
                    binding.btnTldr.isEnabled = true
                }
                is SummaryState.Error -> {
                    binding.layoutSummaryContent.visibility = View.VISIBLE
                    binding.progressSummary.visibility = View.GONE
                    binding.tvSummary.visibility = View.VISIBLE
                    binding.tvSummary.text = "⚠️ ${state.message}"
                    binding.tvSummary.setTextColor(
                        ContextCompat.getColor(ctx, R.color.error)
                    )
                    binding.btnTldr.text = "↺ Retry"
                    binding.btnTldr.isEnabled = true
                }
            }
        }
    }

    companion object {
        private const val BOOKMARK_PAYLOAD = "BOOKMARK"
        private const val SUMMARY_PAYLOAD = "SUMMARY"
    }
}

class FeedDiffCallback : DiffUtil.ItemCallback<ContentItem>() {
    override fun areItemsTheSame(old: ContentItem, new: ContentItem) = old.id == new.id
    override fun areContentsTheSame(old: ContentItem, new: ContentItem) = old == new
}
