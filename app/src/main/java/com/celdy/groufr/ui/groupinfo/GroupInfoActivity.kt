package com.celdy.groufr.ui.groupinfo

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.celdy.groufr.R
import com.celdy.groufr.data.groups.GroupDetailDto
import com.celdy.groufr.databinding.ActivityGroupInfoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@AndroidEntryPoint
class GroupInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupInfoBinding
    private val viewModel: GroupInfoViewModel by viewModels()
    private val memberAdapter = GroupMemberAdapter()

    private var groupId: Long = -1L
    private var groupName: String = ""
    private var groupSlug: String? = null
    private var isSoleOwner = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()
        groupSlug = intent.getStringExtra(EXTRA_GROUP_SLUG)

        binding.groupInfoToolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.groupInfoToolbar.updatePadding(top = systemBars.top)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        binding.groupInfoMembersList.layoutManager = LinearLayoutManager(this)
        binding.groupInfoMembersList.adapter = memberAdapter

        binding.groupInfoName.text = groupName

        setupTabs()
        setupActions()
        observeState()

        if (groupId > 0) {
            viewModel.loadGroupInfo(groupSlug, groupId)
        } else {
            binding.groupInfoLoading.isVisible = false
            binding.groupInfoError.isVisible = true
        }
    }

    private fun setupTabs() {
        binding.groupInfoTabInfo.setOnClickListener { showInfoSection() }
        binding.groupInfoTabMembers.setOnClickListener { showMembersSection() }
    }

    private fun showInfoSection() {
        binding.groupInfoInfoSection.isVisible = true
        binding.groupInfoMembersSection.isVisible = false
        binding.groupInfoTabInfoText.setTextColor(getColor(R.color.primary_600))
        binding.groupInfoTabInfoIndicator.isVisible = true
        binding.groupInfoTabMembersText.setTextColor(getColor(R.color.neutral_600))
        binding.groupInfoTabMembersIndicator.isVisible = false
    }

    private fun showMembersSection() {
        binding.groupInfoInfoSection.isVisible = false
        binding.groupInfoMembersSection.isVisible = true
        binding.groupInfoTabInfoText.setTextColor(getColor(R.color.neutral_600))
        binding.groupInfoTabInfoIndicator.isVisible = false
        binding.groupInfoTabMembersText.setTextColor(getColor(R.color.primary_600))
        binding.groupInfoTabMembersIndicator.isVisible = true
    }

    private fun setupActions() {
        binding.groupInfoLeaveCard.setOnClickListener {
            if (isSoleOwner) return@setOnClickListener
            showLeaveDialog()
        }
        binding.groupInfoDeleteCard.setOnClickListener {
            showDeleteDialog()
        }
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            when (state) {
                GroupInfoState.Loading -> {
                    binding.groupInfoLoading.isVisible = true
                    binding.groupInfoContent.isVisible = false
                    binding.groupInfoError.isVisible = false
                }
                is GroupInfoState.Content -> {
                    binding.groupInfoLoading.isVisible = false
                    binding.groupInfoContent.isVisible = true
                    binding.groupInfoError.isVisible = false
                    bindDetail(state.detail, state.isSoleOwner)
                }
                is GroupInfoState.NoDetail -> {
                    binding.groupInfoLoading.isVisible = false
                    binding.groupInfoContent.isVisible = true
                    binding.groupInfoError.isVisible = false
                    bindNoDetail(state.isSoleOwner)
                }
                GroupInfoState.Error -> {
                    binding.groupInfoLoading.isVisible = false
                    binding.groupInfoContent.isVisible = false
                    binding.groupInfoError.isVisible = true
                }
            }
        }

        viewModel.membersState.observe(this) { state ->
            when (state) {
                MembersState.Loading -> {
                    binding.groupInfoMembersLoading.isVisible = true
                    binding.groupInfoMembersEmpty.isVisible = false
                }
                is MembersState.Content -> {
                    binding.groupInfoMembersLoading.isVisible = false
                    memberAdapter.submitList(state.members)
                    binding.groupInfoMembersEmpty.isVisible = state.members.isEmpty()
                }
                MembersState.Error -> {
                    binding.groupInfoMembersLoading.isVisible = false
                    binding.groupInfoMembersEmpty.isVisible = true
                }
            }
        }

        viewModel.actionState.observe(this) { state ->
            when (state) {
                GroupActionState.Idle -> Unit
                GroupActionState.Loading -> Unit
                GroupActionState.LeaveSuccess -> {
                    Toast.makeText(this, R.string.group_info_leave_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                GroupActionState.DeleteSuccess -> {
                    Toast.makeText(this, R.string.group_info_delete_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                GroupActionState.Error -> {
                    Toast.makeText(this, R.string.group_info_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindDetail(detail: GroupDetailDto, sole: Boolean) {
        isSoleOwner = sole
        binding.groupInfoName.text = detail.name
        groupName = detail.name
        binding.groupInfoCreatedValue.text = formatDate(detail.createdAt)
        binding.groupInfoDescriptionValue.text = if (detail.description.isNullOrBlank()) {
            getString(R.string.group_info_no_description)
        } else {
            detail.description
        }
        binding.groupInfoMemberCountValue.text = detail.memberCount.toString()
        bindRoleAccess(detail.yourRole, sole)
    }

    private fun bindNoDetail(sole: Boolean) {
        isSoleOwner = sole
        binding.groupInfoCreatedValue.text = "-"
        binding.groupInfoDescriptionValue.text = getString(R.string.group_info_no_description)
        binding.groupInfoMemberCountValue.text = "-"
        bindRoleAccess(null, sole)
    }

    private fun bindRoleAccess(role: String?, sole: Boolean) {
        val isOwner = role == "owner"
        binding.groupInfoDeleteCard.isVisible = isOwner

        if (sole) {
            binding.groupInfoLeaveText.setTextColor(getColor(R.color.neutral_600))
            binding.groupInfoLeaveSoleOwnerText.isVisible = true
            binding.groupInfoLeaveCard.isClickable = false
            binding.groupInfoLeaveCard.isFocusable = false
        } else {
            binding.groupInfoLeaveText.setTextColor(getColor(R.color.error_500))
            binding.groupInfoLeaveSoleOwnerText.isVisible = false
            binding.groupInfoLeaveCard.isClickable = true
            binding.groupInfoLeaveCard.isFocusable = true
        }
    }

    private fun showLeaveDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.group_info_leave_confirm_title)
            .setMessage(R.string.group_info_leave_confirm_message)
            .setPositiveButton(R.string.group_info_leave) { dialog, _ ->
                viewModel.leaveGroup(groupId)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteDialog() {
        val input = EditText(this).apply {
            hint = groupName
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.group_info_delete_confirm_title)
            .setMessage(R.string.group_info_delete_confirm_message)
            .setView(input)
            .setPositiveButton(R.string.group_info_delete_button, null)
            .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (input.text.toString().trim() == groupName) {
                    viewModel.deleteGroup(groupId)
                    dialog.dismiss()
                } else {
                    input.error = groupName
                }
            }
        }
        dialog.show()
    }

    private fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val parsed = OffsetDateTime.parse(iso)
            parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: DateTimeParseException) {
            iso
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        const val EXTRA_GROUP_SLUG = "extra_group_slug"
    }
}
