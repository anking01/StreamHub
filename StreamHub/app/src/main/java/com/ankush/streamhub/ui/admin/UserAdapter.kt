package com.ankush.streamhub.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ankush.streamhub.data.remote.AppUser
import com.ankush.streamhub.databinding.ItemUserBinding

class UserAdapter(
    private val onRoleToggle: (AppUser) -> Unit
) : ListAdapter<AppUser, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: AppUser) {
            binding.tvUserName.text  = user.name.ifBlank { "No name" }
            binding.tvUserEmail.text = user.email
            binding.chipRole.text    = user.role.uppercase()
            binding.chipRole.setOnClickListener { onRoleToggle(user) }
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<AppUser>() {
    override fun areItemsTheSame(old: AppUser, new: AppUser)    = old.uid == new.uid
    override fun areContentsTheSame(old: AppUser, new: AppUser) = old == new
}
