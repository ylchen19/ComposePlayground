# Type-Safe Compose Navigation

## Table of Contents

- [Setup](#setup)
- [Define routes with Kotlin Serialization](#define-routes-with-kotlin-serialization)
- [NavHost setup](#navhost-setup)
- [Navigating with type safety](#navigating-with-type-safety)
- [Nested navigation graphs](#nested-navigation-graphs)
- [Deep links](#deep-links)

## Setup

`libs.versions.toml`:

```toml
[versions]
navigation = "2.8.5"
serialization = "1.7.3"

[libraries]
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Module `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.navigation.compose)
    implementation(libs.serialization.json)
}
```

## Define routes with Kotlin Serialization

```kotlin
// Routes are type-safe data classes — no more string-based "route/{arg}" patterns
@Serializable
data object ItemListRoute          // no args

@Serializable
data class ItemDetailRoute(        // with args
    val itemId: String,
)

@Serializable
data object SettingsRoute
```

## NavHost setup

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ItemListRoute,
        modifier = modifier,
    ) {
        composable<ItemListRoute> {
            ItemListScreen(
                onNavigateToDetail = { itemId ->
                    navController.navigate(ItemDetailRoute(itemId))
                },
            )
        }

        composable<ItemDetailRoute> { backStackEntry ->
            // Args are automatically deserialized — no manual parsing
            val route = backStackEntry.toRoute<ItemDetailRoute>()
            ItemDetailScreen(
                itemId = route.itemId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen()
        }
    }
}
```

## Navigating with type safety

```kotlin
// Navigate forward
navController.navigate(ItemDetailRoute(itemId = "42"))

// Navigate and clear back stack (e.g. after login)
navController.navigate(ItemListRoute) {
    popUpTo(navController.graph.startDestinationId) { inclusive = true }
    launchSingleTop = true
}

// Navigate with result (use SavedStateHandle)
// In destination:
navController.previousBackStackEntry
    ?.savedStateHandle
    ?.set("selected_id", selectedId)

// In caller:
val result = navController.currentBackStackEntry
    ?.savedStateHandle
    ?.getStateFlow<String?>("selected_id", null)
    ?.collectAsStateWithLifecycle()
```

## Nested navigation graphs

```kotlin
// Group related screens into nested graphs
NavHost(...) {
    navigation<AuthGraphRoute>(startDestination = LoginRoute) {
        composable<LoginRoute> { LoginScreen(...) }
        composable<RegisterRoute> { RegisterScreen(...) }
    }

    navigation<MainGraphRoute>(startDestination = ItemListRoute) {
        composable<ItemListRoute> { ... }
        composable<ItemDetailRoute> { ... }
    }
}

@Serializable data object AuthGraphRoute
@Serializable data object MainGraphRoute
```

## Deep links

```kotlin
composable<ItemDetailRoute>(
    deepLinks = listOf(
        navDeepLink<ItemDetailRoute>(basePath = "https://myapp.com/items"),
    ),
) { ... }

// AndroidManifest.xml
// <intent-filter>
//     <action android:name="android.intent.action.VIEW" />
//     <data android:scheme="https" android:host="myapp.com" android:pathPrefix="/items" />
// </intent-filter>
```

## Gotchas

- **Do NOT use `navigate()` inside `composable {}` body directly** — it runs on every
  recomposition. Always trigger from user actions or `LaunchedEffect`.
- **`popBackStack()` returns Boolean** — false means nothing to pop. Check it if you
  conditionally navigate after popping.
- **Type-safe nav requires `kotlin-serialization` plugin** — forgetting it causes
  confusing "no serializer found" runtime crashes.
