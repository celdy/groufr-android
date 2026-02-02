package com.celdy.groufr.ui.common

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.celdy.groufr.R
import com.celdy.groufr.data.reactions.ReactionContentType
import com.celdy.groufr.data.reactions.ReactionType
import com.celdy.groufr.data.reactions.ReactionsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReactionDialogFragment : DialogFragment() {

    @Inject
    lateinit var reactionsRepository: ReactionsRepository

    private var contentType: String = ReactionContentType.MESSAGE
    private var contentId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentType = arguments?.getString(ARG_CONTENT_TYPE) ?: ReactionContentType.MESSAGE
        contentId = arguments?.getLong(ARG_CONTENT_ID) ?: 0L
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options = reactionOptions()
        val labels = options.map { getString(it.labelRes) }.toTypedArray()

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.reaction_dialog_title)
            .setItems(labels) { _, which ->
                submitReaction(options[which])
            }
            .setNegativeButton(R.string.reaction_dialog_cancel, null)
            .create()
    }

    private fun submitReaction(option: ReactionOption) {
        lifecycleScope.launch {
            try {
                val response = reactionsRepository.toggleReaction(
                    contentType = contentType,
                    contentId = contentId,
                    reactionType = option.type
                )
                val message = if (response.userReaction == null) {
                    getString(R.string.reaction_removed)
                } else {
                    getString(R.string.reaction_added, getString(option.labelRes))
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.reaction_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun reactionOptions(): List<ReactionOption> {
        return listOf(
            ReactionOption(ReactionType.THUMBS_UP, R.string.reaction_thumbs_up),
            ReactionOption(ReactionType.HEART, R.string.reaction_heart),
            ReactionOption(ReactionType.THANKS, R.string.reaction_thanks),
            ReactionOption(ReactionType.SAD, R.string.reaction_sad),
            ReactionOption(ReactionType.SURPRISED, R.string.reaction_surprised)
        )
    }

    data class ReactionOption(
        val type: String,
        val labelRes: Int
    )

    companion object {
        const val TAG = "ReactionDialogFragment"
        private const val ARG_CONTENT_TYPE = "content_type"
        private const val ARG_CONTENT_ID = "content_id"

        fun newInstance(contentType: String, contentId: Long): ReactionDialogFragment {
            return ReactionDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT_TYPE, contentType)
                    putLong(ARG_CONTENT_ID, contentId)
                }
            }
        }
    }
}
