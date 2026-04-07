package com.example.composeplayground.data.model

data class Pokemon(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,
)

data class PokemonDetail(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val height: Int,
    val weight: Int,
    val types: List<String>,
    val abilities: List<PokemonAbility>,
    val stats: List<PokemonStatInfo>,
)

data class PokemonAbility(
    val name: String,
    val isHidden: Boolean,
)

data class PokemonStatInfo(
    val name: String,
    val baseStat: Int,
)

/**
 * 分頁列表結果，取代直接回傳 API DTO。
 *
 * @param pokemon 本頁已映射完成的 domain model 清單
 * @param hasNext 是否還有下一頁（對應 API 回應的 `next` 欄位）
 */
data class PokemonPage(
    val pokemon: List<Pokemon>,
    val hasNext: Boolean,
)
