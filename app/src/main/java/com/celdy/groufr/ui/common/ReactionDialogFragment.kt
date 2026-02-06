package com.celdy.groufr.ui.common

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
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
    private var currentReaction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentType = arguments?.getString(ARG_CONTENT_TYPE) ?: ReactionContentType.MESSAGE
        contentId = arguments?.getLong(ARG_CONTENT_ID) ?: 0L
        currentReaction = arguments?.getString(ARG_CURRENT_REACTION)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_reaction_picker, null)

        val reactionViews = listOf(
            view.findViewById<TextView>(R.id.reaction_thumbs_up) to ReactionType.THUMBS_UP,
            view.findViewById<TextView>(R.id.reaction_heart) to ReactionType.HEART,
            view.findViewById<TextView>(R.id.reaction_thanks) to ReactionType.THANKS,
            view.findViewById<TextView>(R.id.reaction_sad) to ReactionType.SAD,
            view.findViewById<TextView>(R.id.reaction_surprised) to ReactionType.SURPRISED
        )

        for ((textView, type) in reactionViews) {
            if (type == currentReaction) {
                textView.setBackgroundResource(R.drawable.bg_reaction_picker_selected)
            }
            textView.setOnClickListener {
                submitReaction(type)
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.reaction_dialog_title)
            .setView(view)
            .setNegativeButton(R.string.reaction_dialog_cancel, null)
            .create()
    }

    private fun submitReaction(reactionType: String) {
        lifecycleScope.launch {
            try {
                val response = reactionsRepository.toggleReaction(
                    contentType = contentType,
                    contentId = contentId,
                    reactionType = reactionType
                )
                val message = if (response.userReaction == null) {
                    getString(R.string.reaction_removed)
                } else {
                    val label = reactionLabelRes(reactionType)
                    getString(R.string.reaction_added, getString(label))
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(RESULT_CHANGED to true))
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.reaction_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun reactionLabelRes(type: String): Int {
        return when (type) {
            ReactionType.THUMBS_UP -> R.string.reaction_thumbs_up
            ReactionType.HEART -> R.string.reaction_heart
            ReactionType.THANKS -> R.string.reaction_thanks
            ReactionType.SAD -> R.string.reaction_sad
            ReactionType.SURPRISED -> R.string.reaction_surprised
            else -> R.string.reaction_thumbs_up
        }
    }

    companion object {
        const val TAG = "ReactionDialogFragment"
        const val RESULT_KEY = "reaction_result"
        const val RESULT_CHANGED = "changed"
        private const val ARG_CONTENT_TYPE = "content_type"
        private const val ARG_CONTENT_ID = "content_id"
        private const val ARG_CURRENT_REACTION = "current_reaction"

        fun newInstance(
            contentType: String,
            contentId: Long,
            currentReaction: String? = null
        ): ReactionDialogFragment {
            return ReactionDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT_TYPE, contentType)
                    putLong(ARG_CONTENT_ID, contentId)
                    putString(ARG_CURRENT_REACTION, currentReaction)
                }
            }
        }
    }
}
