package com.celdy.groufr.ui.common

import android.graphics.drawable.PictureDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.RawRes
import com.caverock.androidsvg.SVG

fun ImageView.loadSvg(@RawRes resId: Int) {
    val svg = SVG.getFromResource(context, resId)
    val drawable = PictureDrawable(svg.renderToPicture())
    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    setImageDrawable(drawable)
}
