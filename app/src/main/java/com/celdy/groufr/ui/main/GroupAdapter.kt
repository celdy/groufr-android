package com.celdy.groufr.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.data.groups.GroupDto
import com.celdy.groufr.databinding.ItemGroupBinding

class GroupAdapter(
    private val onClick: (GroupDto) -> Unit
) : ListAdapter<GroupDto, GroupAdapter.GroupViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: GroupDto, onClick: (GroupDto) -> Unit) {
            val initials = buildInitials(group.name)
            binding.groupAvatar.text = initials
            binding.groupName.text = group.name
            binding.groupDescription.text = group.description.orEmpty()
            binding.root.setOnClickListener { onClick(group) }
        }

        private fun buildInitials(name: String): String {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return "--"
            val initials = if (trimmed.length >= 2) {
                trimmed.substring(0, 2)
            } else {
                trimmed
            }
            val locales = binding.root.resources.configuration.locales
            val locale = if (locales.isEmpty) java.util.Locale.getDefault() else locales[0]
            return initials.uppercase(locale)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GroupDto>() {
        override fun areItemsTheSame(oldItem: GroupDto, newItem: GroupDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GroupDto, newItem: GroupDto): Boolean {
            return oldItem == newItem
        }
    }
}
