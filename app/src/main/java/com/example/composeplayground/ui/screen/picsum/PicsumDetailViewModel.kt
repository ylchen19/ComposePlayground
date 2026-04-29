package com.example.composeplayground.ui.screen.picsum

import androidx.lifecycle.ViewModel
import com.example.composeplayground.data.model.PicsumPhoto

/**
 * 詳細頁 ViewModel。所需欄位由導航層透過 NavKey 直接攜帶（id/author/width/height），
 * 不再回打 API，避免單張圖片再做一次列表查詢。
 */
class PicsumDetailViewModel(
    photoId: String,
    author: String,
    originalWidth: Int,
    originalHeight: Int,
) : ViewModel() {

    val photo: PicsumPhoto = PicsumPhoto(
        id = photoId,
        author = author,
        originalWidth = originalWidth,
        originalHeight = originalHeight,
        sourceUrl = "",
    )
}
