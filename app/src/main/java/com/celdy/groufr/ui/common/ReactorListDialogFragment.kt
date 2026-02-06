package com.celdy.groufr.ui.common

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.celdy.groufr.R
import com.celdy.groufr.data.reactions.ReactionContentType
import com.celdy.groufr.data.reactions.ReactionsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReactorListDialogFragment : DialogFragment() {

    @Inject
    lateinit var reactionsRepository: ReactionsRepository

    private var contentType: String = ReactionContentType.MESSAGE
    private var contentId: Long = 0L
    private var currentUserId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentType = arguments?.getString(ARG_CONTENT_TYPE) ?: ReactionContentType.MESSAGE
        contentId = arguments?.getLong(ARG_CONTENT_ID) ?: 0L
        currentUserId = arguments?.getLong(ARG_CURRENT_USER_ID) ?: 0L
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mutableListOf(getString(R.string.reaction_list_loading))
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.reaction_list_title)
            .setAdapter(adapter, null)
            .setPositiveButton(R.string.reaction_list_close, null)
            .create()

        lifecycleScope.launch {
            try {
                val detail = reactionsRepository.getReactions(contentType, contentId)
                adapter.clear()
                if (detail.reactors.isEmpty()) {
                    adapter.add(getString(R.string.reaction_list_empty))
                } else {
                    val youSuffix = getString(R.string.reaction_list_you_suffix)
                    for (reactor in detail.reactors) {
                        val name = if (reactor.userId == currentUserId) {
                            "${reactor.userName} $youSuffix"
                        } else {
                            reactor.userName
                        }
                        adapter.add("${reactor.emoji}  $name")
                    }
                }
            } catch (e: Exception) {
                adapter.clear()
                adapter.add(getString(R.string.reaction_error))
            }
        }

        return dialog
    }

    companion object {
        const val TAG = "ReactorListDialogFragment"
        private const val ARG_CONTENT_TYPE = "content_type"
        private const val ARG_CONTENT_ID = "content_id"
        private const val ARG_CURRENT_USER_ID = "current_user_id"

        fun newInstance(
            contentType: String,
            contentId: Long,
            currentUserId: Long
        ): ReactorListDialogFragment {
            return ReactorListDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT_TYPE, contentType)
                    putLong(ARG_CONTENT_ID, contentId)
                    putLong(ARG_CURRENT_USER_ID, currentUserId)
                }
            }
        }
    }
}
