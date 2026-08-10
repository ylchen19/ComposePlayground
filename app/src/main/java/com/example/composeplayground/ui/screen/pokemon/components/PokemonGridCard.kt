package com.example.composeplayground.ui.screen.pokemon.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.composeplayground.data.model.Pokemon
import java.util.Locale

/**
 * 圖鑑卡片。
 *
 * 插圖區**全出血**貼齊卡片上緣，而不是在卡片內再嵌一個有邊距的方框——後者會疊出
 * 「卡片底色／插圖底色／文字區底色」三層深淺相近的灰階，看起來像沒對齊的盒中盒。
 *
 * 高度由結構保證一致（正方形插圖 + 單行名稱 + 單行屬性列），因此同一列的卡片
 * 不會參差不齊，呼叫端也不需要事先量測最長名稱的高度。
 *
 * 列表 API 不附帶屬性（見 `PokemonRepositoryImpl`），[Pokemon.types] 為空時整張卡
 * 退回主題色，不會變成一片死灰。
 */
@Composable
fun PokemonGridCard(
    pokemon: Pokemon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val typeColor = pokemon.types.firstOrNull()?.let { pokemonTypeColors[it] }
    val accent = typeColor ?: scheme.primary

    // 兩層底色都先與 surface 混色成**不透明**的顏色，而不是疊半透明色塊。疊 alpha 會讓
    // 下層顏色透上來相乘，色階變濁、邊界出現看起來像漸層的雜色。
    val cardColor = typeColor?.let { lerp(scheme.surface, it, CARD_TINT) } ?: scheme.surfaceContainerLow
    val artColor = typeColor?.let { lerp(scheme.surface, it, ART_TINT) } ?: scheme.surfaceContainerHighest

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(artColor),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = pokemon.imageUrl,
                contentDescription = pokemon.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = "#${pokemon.id.toString().padStart(3, '0')}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    // 不透明底色：半透明藥丸疊在插圖上會讓底下的圖透出來，數字糊掉。
                    .background(color = scheme.surface, shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = pokemon.name.replaceFirstChar { it.titlecase(Locale.ROOT) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (pokemon.types.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        // 只給下限不給固定高度：寫死高度會把「惡」「鋼」這類筆畫伸得較低的
                        // 字切掉，而卡片高度本來就一致（每顆藥丸的固有高度相同）。
                        .heightIn(min = TYPE_ROW_MIN_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 最多兩個屬性，固定單行——換行會讓同列卡片高度不一致。
                    pokemon.types.take(2).forEach { type -> PokemonTypeLabel(typeName = type) }
                }
            }
        }
    }
}

private val TYPE_ROW_MIN_HEIGHT = 24.dp

/** 與 surface 的混色比例，愈大屬性色愈明顯。 */
private const val CARD_TINT = 0.10f
private const val ART_TINT = 0.22f
