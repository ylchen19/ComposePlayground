package com.example.composeplayground.data.model

import androidx.compose.runtime.Immutable

// @Immutable 向 Compose compiler 聲明所有 public props 不可變，
// 讓 PokemonGridCard / PokemonListItem 在父層 recompose 時可安全跳過重組。
// 未標記時，List<String> 會被推斷為不穩定型別，導致每次父層更新都強制重組。
@Immutable
data class Pokemon(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,
)

@Immutable
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

@Immutable
data class PokemonAbility(
    val name: String,
    val isHidden: Boolean,
)

@Immutable
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
@Immutable
data class PokemonPage(
    val pokemon: List<Pokemon>,
    val hasNext: Boolean,
)

@Immutable
data class EvolutionNode(
    val id: Int,
    val name: String,
    val imageUrl: String,
)
