#!/usr/bin/env python3
"""
Generate Android Clean Architecture feature module scaffold.

Usage:
    python generate_feature.py <feature_name> <base_package> [--di hilt|koin] [--output-dir <dir>]

Example:
    python generate_feature.py user_profile com.myapp --di koin --output-dir ./app/src/main/kotlin
    python generate_feature.py order_history com.myapp --di hilt --output-dir ./app/src/main/kotlin
"""

import argparse
from pathlib import Path


def to_pascal(snake: str) -> str:
    return "".join(word.capitalize() for word in snake.split("_"))


def to_camel(snake: str) -> str:
    pascal = to_pascal(snake)
    return pascal[0].lower() + pascal[1:]


# ─── UI State (DI-agnostic) ───

def generate_ui_state(name: str, package: str) -> str:
    return f'''package {package}.ui

data class {name}UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface {name}UiEvent {{
    data class ShowSnackbar(val message: String) : {name}UiEvent
}}

sealed interface {name}UiAction {{
    data object LoadData : {name}UiAction
    data object Refresh : {name}UiAction
}}
'''


# ─── ViewModel ───

def generate_viewmodel(name: str, package: str, di: str) -> str:
    if di == "hilt":
        imports = """import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject"""
        annotation = "@HiltViewModel"
        constructor = f"class {name}ViewModel @Inject constructor("
    else:
        imports = ""
        annotation = ""
        constructor = f"class {name}ViewModel("

    return f'''package {package}.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
{imports}
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

{annotation}
{constructor}
    // TODO: inject use cases here
) : ViewModel() {{

    private val _uiState = MutableStateFlow({name}UiState())
    val uiState: StateFlow<{name}UiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<{name}UiEvent>()
    val uiEvent: Flow<{name}UiEvent> = _uiEvent.receiveAsFlow()

    init {{
        onAction({name}UiAction.LoadData)
    }}

    fun onAction(action: {name}UiAction) {{
        when (action) {{
            is {name}UiAction.LoadData -> loadData()
            is {name}UiAction.Refresh -> loadData()
        }}
    }}

    private fun loadData() {{
        viewModelScope.launch {{
            _uiState.update {{ it.copy(isLoading = true, error = null) }}
            // TODO: call use case and update state
            _uiState.update {{ it.copy(isLoading = false) }}
        }}
    }}
}}
'''


# ─── Screen ───

def generate_screen(name: str, package: str, di: str) -> str:
    if di == "hilt":
        vm_import = "import androidx.hilt.navigation.compose.hiltViewModel"
        vm_default = f"viewModel: {name}ViewModel = hiltViewModel(),"
    else:
        vm_import = "import org.koin.androidx.compose.koinViewModel"
        vm_default = f"viewModel: {name}ViewModel = koinViewModel(),"

    return f'''package {package}.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
{vm_import}

@Composable
fun {name}Screen(
    {vm_default}
) {{
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember {{ SnackbarHostState() }}

    LaunchedEffect(Unit) {{
        viewModel.uiEvent.collect {{ event ->
            when (event) {{
                is {name}UiEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
            }}
        }}
    }}

    Scaffold(snackbarHost = {{ SnackbarHost(snackbarHostState) }}) {{ padding ->
        {name}Content(
            uiState = uiState,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(padding),
        )
    }}
}}

@Composable
private fun {name}Content(
    uiState: {name}UiState,
    onAction: ({name}UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {{
    // TODO: implement UI
}}
'''


# ─── Repository ───

def generate_repository_interface(name: str, package: str) -> str:
    return f'''package {package}.domain.repository

interface {name}Repository {{
    // TODO: define repository contract
}}
'''


def generate_repository_impl(name: str, package: str, di: str) -> str:
    if di == "hilt":
        import_line = "\nimport javax.inject.Inject"
        constructor = f"class {name}RepositoryImpl @Inject constructor("
    else:
        import_line = ""
        constructor = f"class {name}RepositoryImpl("

    return f'''package {package}.data

import {package}.domain.repository.{name}Repository{import_line}

{constructor}
    // TODO: inject ApiService, Dao, Mapper
) : {name}Repository {{
    // TODO: implement repository
}}
'''


# ─── DI Module ───

def generate_di_module_hilt(name: str, package: str) -> str:
    return f'''package {package}.data.di

import {package}.data.{name}RepositoryImpl
import {package}.domain.repository.{name}Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class {name}Module {{

    @Binds
    abstract fun bind{name}Repository(
        impl: {name}RepositoryImpl,
    ): {name}Repository
}}
'''


def generate_di_module_koin(name: str, package: str, snake_name: str) -> str:
    camel = to_camel(snake_name)
    return f'''package {package}.di

import {package}.data.{name}RepositoryImpl
import {package}.domain.repository.{name}Repository
import {package}.ui.{name}ViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val {camel}Module = module {{
    // Data layer
    single<{name}Repository> {{ {name}RepositoryImpl(/* get(), get() */) }}

    // Domain layer
    // factory {{ Get{name}UseCase(get()) }}

    // UI layer
    viewModel {{ {name}ViewModel(/* get() */) }}
}}
'''


def generate_koin_check_test(name: str, package: str, snake_name: str) -> str:
    camel = to_camel(snake_name)
    return f'''package {package}

import {package}.di.{camel}Module
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules

/**
 * Verifies all Koin dependencies resolve correctly.
 * Run this in CI to catch missing bindings early.
 */
class {name}ModuleCheckTest : KoinTest {{

    @Test
    fun `verify {camel}Module dependencies resolve`() {{
        koinApplication {{
            modules({camel}Module)
        }}.checkModules()
    }}
}}
'''


def main():
    parser = argparse.ArgumentParser(description="Generate Android feature scaffold")
    parser.add_argument("feature_name", help="Feature name in snake_case (e.g. user_profile)")
    parser.add_argument("base_package", help="Base package (e.g. com.myapp.feature)")
    parser.add_argument("--di", choices=["hilt", "koin"], default="hilt",
                        help="DI framework: hilt (default) or koin")
    parser.add_argument("--output-dir", default=".", help="Output directory")
    args = parser.parse_args()

    name = to_pascal(args.feature_name)
    pkg = f"{args.base_package}.{args.feature_name.replace('_', '')}"
    pkg_path = pkg.replace(".", "/")
    base = Path(args.output_dir) / pkg_path
    di = args.di

    files = {
        f"ui/{name}UiState.kt": generate_ui_state(name, pkg),
        f"ui/{name}ViewModel.kt": generate_viewmodel(name, pkg, di),
        f"ui/{name}Screen.kt": generate_screen(name, pkg, di),
        f"domain/repository/{name}Repository.kt": generate_repository_interface(name, pkg),
        f"data/{name}RepositoryImpl.kt": generate_repository_impl(name, pkg, di),
    }

    if di == "hilt":
        files[f"data/di/{name}Module.kt"] = generate_di_module_hilt(name, pkg)
    else:
        files[f"di/{name}Module.kt"] = generate_di_module_koin(name, pkg, args.feature_name)
        files[f"di/{name}ModuleCheckTest.kt"] = generate_koin_check_test(name, pkg, args.feature_name)

    created = []
    for rel_path, content in files.items():
        full_path = base / rel_path
        full_path.parent.mkdir(parents=True, exist_ok=True)
        full_path.write_text(content, encoding="utf-8")
        created.append(str(full_path))

    print(f"\n✅ Feature '{name}' scaffolded with [{di.upper()}] DI!")
    print(f"📦 Package: {pkg}")
    print(f"📁 Files created:")
    for f in created:
        print(f"   {f}")

    if di == "koin":
        camel = to_camel(args.feature_name)
        print(f"\n💡 Next step: register '{camel}Module' in your Application's startKoin {{ }} block.")
    else:
        print(f"\n💡 Next step: ensure @HiltAndroidApp is on your Application class.")


if __name__ == "__main__":
    main()
