package com.celdy.groufr.ui.common

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan

object MarkdownRenderer {

    fun render(source: String): CharSequence {
        if (source.isBlank()) return ""
        val builder = SpannableStringBuilder(source)
        applyLineFormatting(builder)
        applyMarkdownPattern(builder, "\\*\\*(.+?)\\*\\*") { start, end ->
            builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        applyMarkdownPattern(builder, "__(.+?)__") { start, end ->
            builder.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        applyMarkdownPattern(builder, "\\*(.+?)\\*") { start, end ->
            builder.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        applyMarkdownPattern(builder, "_(.+?)_") { start, end ->
            builder.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        applyMarkdownPattern(builder, "`([^`]+)`") { start, end ->
            builder.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        applyMarkdownPattern(builder, "\\[([^\\]]+)]\\(([^)]+)\\)") { start, end, groups ->
            builder.setSpan(URLSpan(groups[2]), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        applyAutoLinks(builder)
        return builder
    }

    private fun applyAutoLinks(builder: SpannableStringBuilder) {
        val urlPattern = Regex("(?<![\\[\\(])https?://[^\\s\\)\\]]+")
        val existingSpans = builder.getSpans(0, builder.length, URLSpan::class.java)
        val coveredRanges = existingSpans.map {
            builder.getSpanStart(it)..builder.getSpanEnd(it)
        }
        val matches = urlPattern.findAll(builder.toString()).toList()
        for (match in matches.asReversed()) {
            val start = match.range.first
            val end = match.range.last + 1
            val alreadyCovered = coveredRanges.any { range ->
                start >= range.first && end <= range.last
            }
            if (!alreadyCovered) {
                builder.setSpan(URLSpan(match.value), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun applyLineFormatting(builder: SpannableStringBuilder) {
        val text = builder.toString()
        var offset = 0
        val ranges = mutableListOf<Pair<Int, Int>>()
        for (line in text.split("\n")) {
            val end = offset + line.length
            ranges.add(offset to end)
            offset = end + 1
        }

        for (range in ranges.asReversed()) {
            val start = range.first
            val end = range.second
            if (start >= end) continue
            val line = builder.substring(start, end)
            when {
                line.startsWith("### ") -> {
                    val prefix = 4
                    builder.delete(start, start + prefix)
                    val adjustedEnd = end - prefix
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(RelativeSizeSpan(1.1f), start, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("## ") -> {
                    val prefix = 3
                    builder.delete(start, start + prefix)
                    val adjustedEnd = end - prefix
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(RelativeSizeSpan(1.2f), start, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("# ") -> {
                    val prefix = 2
                    builder.delete(start, start + prefix)
                    val adjustedEnd = end - prefix
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(RelativeSizeSpan(1.3f), start, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("> ") -> {
                    val prefix = 2
                    builder.delete(start, start + prefix)
                    val adjustedEnd = end - prefix
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(LeadingMarginSpan.Standard(32), start, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("- ") || line.startsWith("* ") || line.matches(Regex("\\d+\\. .*")) -> {
                    builder.setSpan(LeadingMarginSpan.Standard(32), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun applyMarkdownPattern(
        builder: SpannableStringBuilder,
        pattern: String,
        apply: (start: Int, end: Int) -> Unit
    ) {
        applyMarkdownPattern(builder, pattern) { start, end, _ ->
            apply(start, end)
        }
    }

    private fun applyMarkdownPattern(
        builder: SpannableStringBuilder,
        pattern: String,
        apply: (start: Int, end: Int, groups: List<String>) -> Unit
    ) {
        val regex = Regex(pattern)
        val matches = regex.findAll(builder.toString()).toList()
        for (match in matches.asReversed()) {
            val groups = match.groupValues
            if (groups.size < 2) continue
            val replacement = groups[1]
            val start = match.range.first
            builder.replace(match.range.first, match.range.last + 1, replacement)
            val end = start + replacement.length
            apply(start, end, groups)
        }
    }
}
