package com.example.composeplayground.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { onNavigateToDetail("item-001") }) {
            Text("Go to Detail (item-001)")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { onNavigateToDetail("item-002") }) {
            Text("Go to Detail (item-002)")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onNavigateToSettings) {
            Text("Go to Settings")
        }
    }
}
