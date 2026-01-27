package com.celdy.groufr.ui.common

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.celdy.groufr.R
import com.celdy.groufr.data.reports.ReportContentType
import com.celdy.groufr.data.reports.ReportReason
import com.celdy.groufr.data.reports.ReportsRepository
import com.celdy.groufr.databinding.DialogReportBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@AndroidEntryPoint
class ReportDialogFragment : DialogFragment() {

    @Inject
    lateinit var reportsRepository: ReportsRepository

    private var _binding: DialogReportBinding? = null
    private val binding get() = _binding!!

    private var contentType: String = ReportContentType.MESSAGE
    private var contentId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentType = arguments?.getString(ARG_CONTENT_TYPE) ?: ReportContentType.MESSAGE
        contentId = arguments?.getLong(ARG_CONTENT_ID) ?: 0L
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogReportBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.report_dialog_title)
            .setView(binding.root)
            .setPositiveButton(R.string.report_dialog_submit, null)
            .setNegativeButton(R.string.report_dialog_cancel) { _, _ -> dismiss() }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                submitReport()
            }
        }

        return dialog
    }

    private fun submitReport() {
        val reason = getSelectedReason()
        if (reason == null) {
            Toast.makeText(requireContext(), R.string.report_dialog_reason_label, Toast.LENGTH_SHORT).show()
            return
        }

        val comment = binding.reportCommentInput.text?.toString()?.takeIf { it.isNotBlank() }

        binding.reportLoading.isVisible = true
        binding.reportReasonGroup.isEnabled = false
        binding.reportCommentInput.isEnabled = false
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false

        lifecycleScope.launch {
            try {
                reportsRepository.createReport(
                    contentType = contentType,
                    contentId = contentId,
                    reason = reason,
                    comment = comment
                )
                Toast.makeText(requireContext(), R.string.report_success, Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    409 -> {
                        val errorBody = e.response()?.errorBody()?.string().orEmpty()
                        when {
                            errorBody.contains("already_reported") -> R.string.report_error_already_reported
                            errorBody.contains("cannot_report_own_content") -> R.string.report_error_own_content
                            else -> R.string.report_error
                        }
                    }
                    else -> R.string.report_error
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                resetUI()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.report_error, Toast.LENGTH_SHORT).show()
                resetUI()
            }
        }
    }

    private fun resetUI() {
        binding.reportLoading.isVisible = false
        binding.reportReasonGroup.isEnabled = true
        binding.reportCommentInput.isEnabled = true
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
    }

    private fun getSelectedReason(): String? {
        return when (binding.reportReasonGroup.checkedRadioButtonId) {
            R.id.report_reason_spam -> ReportReason.SPAM
            R.id.report_reason_harassment -> ReportReason.HARASSMENT
            R.id.report_reason_hate_speech -> ReportReason.HATE_SPEECH
            R.id.report_reason_illegal -> ReportReason.ILLEGAL
            R.id.report_reason_violence -> ReportReason.VIOLENCE
            R.id.report_reason_inappropriate -> ReportReason.INAPPROPRIATE
            R.id.report_reason_other -> ReportReason.OTHER
            else -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReportDialogFragment"
        private const val ARG_CONTENT_TYPE = "content_type"
        private const val ARG_CONTENT_ID = "content_id"

        fun newInstance(contentType: String, contentId: Long): ReportDialogFragment {
            return ReportDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT_TYPE, contentType)
                    putLong(ARG_CONTENT_ID, contentId)
                }
            }
        }
    }
}
