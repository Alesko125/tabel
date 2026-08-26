package com.example.tabel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tabel.ui.MonthScreen
import com.example.tabel.ui.SalaryScreen
import com.example.tabel.ui.SettingsScreen
import com.example.tabel.ui.TodayScreen
import com.example.tabel.ui.theme.TabelTheme
import java.time.LocalDate

private enum class Tab(val title: String, val icon: ImageVector) {
    TODAY("Зміна", Icons.Outlined.CheckCircle),
    MONTH("Місяць", Icons.Filled.DateRange),
    SALARY("Зарплата", Icons.Filled.Star)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val state by vm.state.collectAsState()
            TabelTheme(themeMode = state.themeMode) {
                TabelApp(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabelApp(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.TODAY) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showSettings) "Налаштування"
                        else when (tab) {
                            Tab.TODAY -> "Хто на зміні"
                            Tab.MONTH -> "Мій графік"
                            Tab.SALARY -> "Зарплата"
                        }
                    )
                },
                actions = {
                    if (!showSettings) {
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Оновити")
                        }
                    }
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Налаштування")
                    }
                }
            )
        },
        bottomBar = {
            if (!showSettings) {
                NavigationBar {
                    for (item in Tab.entries) {
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                showSettings -> SettingsScreen(
                    state = state,
                    onThemeMode = vm::setThemeMode,
                    onName = vm::setMyName,
                    onBase = vm::setBase,
                    onBonus = vm::setBonus,
                    onDobaStart = vm::setDobaStart,
                    onDayStart = vm::setDayStart,
                    onSheetId = vm::setSheetId,
                    onRefresh = vm::refresh
                )

                tab == Tab.TODAY -> TodayScreen(
                    state = state,
                    onShiftDay = vm::shiftDay,
                    onToday = { vm.selectDate(LocalDate.now()) }
                )

                tab == Tab.MONTH -> MonthScreen(
                    state = state,
                    onShiftMonth = vm::shiftMonth,
                    onSelectDate = { date ->
                        vm.selectDate(date)
                        tab = Tab.TODAY
                    }
                )

                tab == Tab.SALARY -> SalaryScreen(
                    state = state,
                    onShiftMonth = vm::shiftMonth
                )
            }
        }
    }
}
