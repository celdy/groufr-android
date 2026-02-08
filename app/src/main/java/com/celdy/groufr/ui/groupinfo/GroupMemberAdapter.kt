package com.celdy.groufr.ui.groupinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.groups.GroupMemberDto
import com.celdy.groufr.databinding.ItemGroupMemberBinding
import com.celdy.groufr.ui.common.AvatarHelper

class GroupMemberAdapter :
    ListAdapter<GroupMemberDto, GroupMemberAdapter.MemberViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemberViewHolder(
        private val binding: ItemGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(member: GroupMemberDto) {
            val context = binding.root.context
            AvatarHelper.bindAvatar(binding.memberAvatar, member.user.name)
            binding.memberName.text = member.user.name
            binding.memberRoleBadge.text = when (member.role) {
                "owner" -> context.getString(R.string.group_info_role_owner)
                "admin" -> context.getString(R.string.group_info_role_admin)
                else -> context.getString(R.string.group_info_role_member)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GroupMemberDto>() {
        override fun areItemsTheSame(
            oldItem: GroupMemberDto,
            newItem: GroupMemberDto
        ): Boolean {
            return oldItem.user.id == newItem.user.id
        }

        override fun areContentsTheSame(
            oldItem: GroupMemberDto,
            newItem: GroupMemberDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}
