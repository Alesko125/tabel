package com.example.tabel

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tabel.ui.*
import com.example.tabel.ui.theme.TabelTheme
import com.example.tabel.work.Notifications
import java.time.LocalDate

private enum class Tab(val title: String, val icon: ImageVector) {
    TODAY("Зміна", Icons.Outlined.CheckCircle),
    MONTH("Місяць", Icons.Filled.DateRange),
    PEOPLE("Люди", Icons.AutoMirrored.Filled.List),
    STATS("Підсумки", Icons.Outlined.Info),
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

    RequestNotificationPermission(enabled = state.notifyShift || state.notifyChanges)

    val personOpen = state.selectedPerson != null

    // Свайп «назад» іде вглиб екранів, а не одразу з застосунку.
    BackHandler(enabled = personOpen) { vm.selectPerson(null) }
    BackHandler(enabled = !personOpen && showSettings) { showSettings = false }
    BackHandler(enabled = !personOpen && !showSettings && tab != Tab.TODAY) { tab = Tab.TODAY }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            personOpen -> state.selectedPerson.orEmpty()
                            showSettings -> "Налаштування"
                            else -> when (tab) {
                                Tab.TODAY -> "Хто на зміні"
                                Tab.MONTH -> "Мій графік"
                                Tab.PEOPLE -> "Люди"
                                Tab.STATS -> "Підсумки"
                                Tab.SALARY -> "Зарплата"
                            }
                        }
                    )
                },
                actions = {
                    if (!showSettings && !personOpen) {
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Оновити")
                        }
                    }
                    if (!personOpen) {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Налаштування")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!showSettings && !personOpen) {
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                personOpen -> PersonScreen(
                    state = state,
                    onSetStartHour = vm::setPersonStartHour
                )

                showSettings -> SettingsScreen(
                    state = state,
                    onThemeMode = vm::setThemeMode,
                    onName = vm::setMyName,
                    onBase = vm::setBase,
                    onBonus = vm::setBonus,
                    onDobaStart = vm::setDobaStart,
                    onDayStart = vm::setDayStart,
                    onSheetId = vm::setSheetId,
                    onRefresh = vm::refresh,
                    onNotifyShift = vm::setNotifyShift,
                    onNotifyChanges = vm::setNotifyChanges,
                    onReminderHour = vm::setReminderHour,
                    onRemindOnShiftDay = vm::setRemindOnShiftDay
                )

                tab == Tab.TODAY -> TodayScreen(
                    state = state,
                    onShiftDay = vm::shiftDay,
                    onToday = { vm.selectDate(LocalDate.now()) },
                    onSelectPerson = vm::selectPerson,
                    onDismissChange = vm::dismissScheduleChange
                )

                tab == Tab.MONTH -> MonthScreen(
                    state = state,
                    onShiftMonth = vm::shiftMonth,
                    onSelectDate = { date ->
                        vm.selectDate(date)
                        tab = Tab.TODAY
                    }
                )

                tab == Tab.PEOPLE -> PeopleScreen(
                    state = state,
                    onSelectPerson = vm::selectPerson
                )

                tab == Tab.STATS -> StatsScreen(
                    state = state,
                    onLoad = vm::loadStats
                )

                tab == Tab.SALARY -> SalaryScreen(
                    state = state,
                    onShiftMonth = vm::shiftMonth,
                    onLoadHistory = { vm.loadHistory() },
                    onSetActual = vm::setActualPay
                )
            }
        }
    }
}

/** На Android 13+ без дозволу сповіщення мовчки не з'являються. */
@Composable
private fun RequestNotificationPermission(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(enabled) {
        if (enabled && !Notifications.canPost(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
