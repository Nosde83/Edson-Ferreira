package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.BibleStudent
import com.example.data.DailyPlan
import com.example.data.MagazinePlacement
import com.example.data.ServiceReport
import com.example.ui.MinistryViewModel
import kotlin.math.roundToInt

// Portuguese Month Names mapped from index 1-12
val monthsPt = listOf(
    "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

enum class AppTab(val title: String, val testTag: String) {
    DASHBOARD("Início", "tab_dashboard"),
    PLANNER("Planos", "tab_planner"),
    STUDENTS("Estudantes", "tab_students"),
    PLACEMENTS("Revistas", "tab_placements"),
    PROFILE("Perfil", "tab_profile")
}

@Composable
fun MainAppScreen(
    viewModel: MinistryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val timerFinishedAlert by viewModel.showTimerFinishedAlert.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    // Onboarding if user has not set their name yet
    if (profile.name.isBlank()) {
        OnboardingScreen(
            onRegister = { name, role, goal, dueDay ->
                viewModel.updateProfile(name, role, goal, dueDay)
                Toast.makeText(context, "Cadastro concluído com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    AppTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        val icon = when (tab) {
                            AppTab.DASHBOARD -> if (isSelected) Icons.Default.Home else Icons.Outlined.Home
                            AppTab.PLANNER -> if (isSelected) Icons.Default.DateRange else Icons.Outlined.DateRange
                            AppTab.STUDENTS -> if (isSelected) Icons.Default.School else Icons.Outlined.School
                            AppTab.PLACEMENTS -> if (isSelected) Icons.Default.ChromeReaderMode else Icons.Outlined.ChromeReaderMode
                            AppTab.PROFILE -> if (isSelected) Icons.Default.Person else Icons.Outlined.Person
                        }
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    AppTab.PLANNER -> PlannerScreen(viewModel = viewModel)
                    AppTab.STUDENTS -> StudentsScreen(viewModel = viewModel)
                    AppTab.PLACEMENTS -> PlacementsScreen(viewModel = viewModel)
                    AppTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                }

                // Alert overlay for completed timer
                if (timerFinishedAlert) {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearFinishedAlert() },
                        title = { Text("⏰ Tempo Concluído!", fontWeight = FontWeight.Bold) },
                        text = { Text("O seu cronómetro terminou a contagem regressiva. Gostaria de registar as suas horas no relatório?", fontSize = 15.sp) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.clearFinishedAlert()
                                    currentTab = AppTab.DASHBOARD
                                },
                                modifier = Modifier.testTag("alert_register_hours")
                            ) {
                                Text("Registar Relatório")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.clearFinishedAlert() }) {
                                Text("Fechar")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onRegister: (String, String, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Publicador") }
    var goalHours by remember { mutableStateOf("15") }
    var dueDay by remember { mutableStateOf("28") }
    
    val roles = listOf("Publicador", "Pioneiro Auxiliar", "Pioneiro Regular", "Pioneiro Especial")
    var rolesExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(30.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.onboarding_hero),
                    contentDescription = "Bíblia e pregação",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Text(
                text = "Meu Ministério",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "Organize a sua pregação individual de forma simples, elegante e focada.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome Completo") },
                placeholder = { Text("Introduza o seu nome") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = rolesExpanded,
                onExpandedChange = { rolesExpanded = !rolesExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Modalidade de Serviço") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rolesExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = rolesExpanded,
                    onDismissRequest = { rolesExpanded = false }
                ) {
                    roles.forEach { roleOption ->
                        DropdownMenuItem(
                            text = { Text(roleOption) },
                            onClick = {
                                role = roleOption
                                goalHours = when(roleOption) {
                                    "Pioneiro Regular" -> "50"
                                    "Pioneiro Auxiliar" -> "30"
                                    "Publicador" -> "15"
                                    "Pioneiro Especial" -> "100"
                                    else -> "15"
                                }
                                rolesExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = goalHours,
                    onValueChange = { goalHours = it },
                    label = { Text("Meta Horas Mensal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_goal_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { dueDay = it },
                    label = { Text("Dia Limite Relatório") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_due_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val goal = goalHours.toIntOrNull() ?: 15
                        val due = dueDay.toIntOrNull() ?: 28
                        onRegister(name.trim(), role, goal, due)
                    } else {
                        onRegister("Publicador", role, 15, 28)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_submit_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Entrar no Ministério", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MinistryViewModel) {
    val context = LocalContext.current
    val stats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val reports by viewModel.reportsState.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()

    var showAddReportDialog by remember { mutableStateOf(false) }

    val timerRemaining by viewModel.timerRemainingSeconds.collectAsStateWithLifecycle()
    val timerOriginal by viewModel.timerOriginalSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()

    val todayDay = viewModel.currentDay
    val currentCalendarMonth = viewModel.currentMonth
    val currentCalendarYear = viewModel.currentYear
    
    val isNearReportDue = selectedYear == currentCalendarYear && 
            selectedMonth == currentCalendarMonth && 
            todayDay >= profile.reportDueDay

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isNearReportDue) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val reportText = viewModel.generateShareReportText(monthsPt[selectedMonth])
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Enviar via WhatsApp / SMS"))
                        }
                        .testTag("report_delivery_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("🚨 Dia de Entrega Próximo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Hoje é dia $todayDay. A sua meta de entrega é dia ${profile.reportDueDay}. Toque aqui para partilhar o relatório via WhatsApp ou SMS!", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.changeMonth(-1) },
                        modifier = Modifier.testTag("prev_month_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês Anterior")
                    }
                    Text(
                        text = "${monthsPt[selectedMonth]} de $selectedYear",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { viewModel.changeMonth(1) },
                        modifier = Modifier.testTag("next_month_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mês Seguinte")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(180.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                            val progress = stats.progressPercentage.toFloat() / 100f
                            val stroke = Stroke(width = 8.dp.value, cap = StrokeCap.Round)
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val secondaryColor = MaterialTheme.colorScheme.secondary
                            val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = stroke
                                )
                                drawArc(
                                    brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                                    startAngle = 135f,
                                    sweepAngle = 270f * progress,
                                    useCenter = false,
                                    style = stroke
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${String.format("%.1f", stats.totalHours)}h",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Meta: ${stats.targetHours.roundToInt()}h",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Progresso: ${(stats.progressPercentage).roundToInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(180.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatRowItem(icon = Icons.Default.Book, label = "Publicações", value = stats.totalMagazines.toString(), color = MaterialTheme.colorScheme.primary)
                        StatRowItem(icon = Icons.Default.PlayCircle, label = "Vídeos", value = stats.totalVideos.toString(), color = MaterialTheme.colorScheme.secondary)
                        StatRowItem(icon = Icons.Default.Group, label = "Revisitas", value = stats.totalReturnVisits.toString(), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = "Cronómetro", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cronómetro de Campo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                Text(
                                    text = "${mins}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { viewModel.setTimerDuration(mins) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val minutesLeft = timerRemaining / 60
                    val secondsLeft = timerRemaining % 60
                    val timerDisplay = String.format("%02d:%02d", minutesLeft, secondsLeft)

                    Text(
                        text = timerDisplay,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("timer_display_text")
                    )

                    val timerPercentage = if (timerOriginal > 0) timerRemaining.toFloat() / timerOriginal else 0f
                    LinearProgressIndicator(
                        progress = { timerPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.resetTimer() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .testTag("timer_reset_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.primary)
                        }

                        Button(
                            onClick = {
                                if (isTimerRunning) viewModel.pauseTimer() else viewModel.startTimer()
                            },
                            modifier = Modifier.testTag("timer_play_pause_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Controle")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isTimerRunning) "Pausar" else "Iniciar")
                        }

                        if (timerOriginal - timerRemaining >= 60) {
                            IconButton(
                                onClick = {
                                    val elapsedHours = (timerOriginal - timerRemaining).toDouble() / 3600.0
                                    viewModel.addReport(
                                        day = viewModel.currentDay,
                                        hours = String.format("%.2f", elapsedHours).replace(",", ".").toDoubleOrNull() ?: 1.0,
                                        magazines = 0,
                                        videos = 0,
                                        returnVisits = 0,
                                        notes = "Atividades com cronómetro"
                                    )
                                    viewModel.resetTimer()
                                    Toast.makeText(context, "Tempo registado com sucesso!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                    .testTag("timer_save_hours_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Registar Horas", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showAddReportDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_activity_fab"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Atividade")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registar Atividade", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val reportText = viewModel.generateShareReportText(monthsPt[selectedMonth])
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Enviar via WhatsApp / SMS"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_whatsapp_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Partilhar Relatório")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar WhatsApp/SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(
                text = "Registos deste Mês",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (reports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Ainda não registou atividades de ministério este mês. Use o cronómetro ou clique em \"Registar Atividade\"!",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(reports) { report ->
                ReportListItem(
                    report = report,
                    onDelete = { viewModel.deleteReport(report.id) }
                )
            }
        }
    }

    if (showAddReportDialog) {
        AddReportDialog(
            daySelected = viewModel.currentDay,
            onDismiss = { showAddReportDialog = false },
            onSave = { day, hrs, mags, vids, revisits, comment ->
                viewModel.addReport(day, hrs, mags, vids, revisits, comment)
                showAddReportDialog = false
                Toast.makeText(context, "Relatório registado com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun StatRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReportListItem(
    report: ServiceReport,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dia ${report.day} de ${monthsPt[report.month]}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BadgeText(text = "⏱️ ${report.hours} h")
                    if (report.magazines > 0) BadgeText(text = "📚 ${report.magazines} pub.")
                    if (report.videos > 0) BadgeText(text = "🎥 ${report.videos} víd.")
                    if (report.returnVisits > 0) BadgeText(text = "♻️ ${report.returnVisits} rev.")
                }
                
                if (report.notes.isNotBlank()) {
                    Text(
                        text = "Nota: ${report.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_report_btn")) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun BadgeText(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun AddReportDialog(
    daySelected: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Double, Int, Int, Int, String) -> Unit
) {
    var day by remember { mutableStateOf(daySelected.toString()) }
    var hours by remember { mutableStateOf("") }
    var magazines by remember { mutableStateOf("0") }
    var videos by remember { mutableStateOf("0") }
    var returnVisits by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Registar Atividade de Campo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }

                item {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("Dia do Mês") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("Horas Realizadas") },
                        placeholder = { Text("Ex: 1.5 ou 2") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_hours_input")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = magazines,
                            onValueChange = { magazines = it },
                            label = { Text("Publicações") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = videos,
                            onValueChange = { videos = it },
                            label = { Text("Vídeos Mostrados") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = returnVisits,
                        onValueChange = { returnVisits = it },
                        label = { Text("Revisitas Efetuadas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas / Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val d = day.toIntOrNull() ?: daySelected
                                val h = hours.replace(",", ".").toDoubleOrNull() ?: 1.0
                                val m = magazines.toIntOrNull() ?: 0
                                val v = videos.toIntOrNull() ?: 0
                                val r = returnVisits.toIntOrNull() ?: 0
                                onSave(d, h, m, v, r, notes)
                            },
                            modifier = Modifier.testTag("dialog_save_btn")
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PLANNER SCREEN (PLANIFICAÇÃO DIÁRIA)
// -------------------------------------------------------------
@Composable
fun PlannerScreen(viewModel: MinistryViewModel) {
    val plans by viewModel.plansState.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    var showAddPlanDialog by remember { mutableStateOf(false) }

    val monthlyPlans = plans.filter { it.year == selectedYear && it.month == selectedMonth }

    Scaffold(
        modifier = Modifier.testTag("planner_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPlanDialog = true },
                modifier = Modifier.testTag("planner_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Planear Dia")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Planificação das Actividades",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Agende os seus dias de pregação e organize companheiros de serviço.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (monthlyPlans.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Empty", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Nenhum plano registado para ${monthsPt[selectedMonth]} de $selectedYear.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(monthlyPlans) { plan ->
                    DailyPlanItem(
                        plan = plan,
                        onToggle = { viewModel.togglePlanCompletion(plan) },
                        onDelete = { viewModel.deletePlan(plan) }
                    )
                }
            }
        }
    }

    if (showAddPlanDialog) {
        Dialog(onDismissRequest = { showAddPlanDialog = false }) {
            var dayInput by remember { mutableStateOf("") }
            var activityInput by remember { mutableStateOf("") }
            var companionInput by remember { mutableStateOf("") }
            var notesInput by remember { mutableStateOf("") }

            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Planificar Serviço de Campo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = dayInput,
                        onValueChange = { dayInput = it },
                        label = { Text("Dia do Mês") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("planner_dialog_day")
                    )

                    OutlinedTextField(
                        value = activityInput,
                        onValueChange = { activityInput = it },
                        label = { Text("Atividade Planeada") },
                        placeholder = { Text("Ex: Carrinho pública, de Casa em Casa") },
                        modifier = Modifier.fillMaxWidth().testTag("planner_dialog_activity")
                    )

                    OutlinedTextField(
                        value = companionInput,
                        onValueChange = { companionInput = it },
                        label = { Text("Companheiro (Parceiro)") },
                        placeholder = { Text("Nome do irmão/irmã") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notas de Território / Ponto") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddPlanDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val d = dayInput.toIntOrNull() ?: 1
                                viewModel.addPlan(d, activityInput, companionInput, notesInput)
                                showAddPlanDialog = false
                            },
                            modifier = Modifier.testTag("planner_dialog_save")
                        ) {
                            Text("Agendar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyPlanItem(
    plan: DailyPlan,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isCompleted) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = plan.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("plan_checkbox_${plan.id}")
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dia ${plan.day} de ${monthsPt[plan.month]}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (plan.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = plan.plannedActivity,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (plan.companion.isNotBlank()) {
                    Text(
                        text = "👥 Companheiro: ${plan.companion}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (plan.notes.isNotBlank()) {
                    Text(
                        text = "📌 ${plan.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}

// -------------------------------------------------------------
// BIBLE STUDENTS SCREEN (ACOMPANHAMENTO DOS ESTUDANTES)
// -------------------------------------------------------------
@Composable
fun StudentsScreen(viewModel: MinistryViewModel) {
    val students by viewModel.studentsState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag("students_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("students_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Estudante")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Estudantes da Bíblia",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Acompanhe o preenchimento, progresso e agenda de seus estudantes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (students.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.School, contentDescription = "Empty", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Ainda não registou estudantes da Bíblia.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(students) { student ->
                    StudentListItem(
                        student = student,
                        onDelete = { viewModel.deleteStudent(student) },
                        onToggleActive = { active ->
                            viewModel.addStudent(
                                name = student.name,
                                book = student.bookOrTopic,
                                lesson = student.currentLesson,
                                schedule = student.studySchedule,
                                notes = student.notes,
                                isActive = active
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            var nameInput by remember { mutableStateOf("") }
            var bookInput by remember { mutableStateOf("Seja Feliz Para Sempre!") }
            var lessonInput by remember { mutableStateOf("Lição 1") }
            var scheduleInput by remember { mutableStateOf("") }
            var notesInput by remember { mutableStateOf("") }

            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Registar Estudante da Bíblia", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nome do Estudante") },
                        modifier = Modifier.fillMaxWidth().testTag("student_dialog_name")
                    )

                    OutlinedTextField(
                        value = bookInput,
                        onValueChange = { bookInput = it },
                        label = { Text("Livro / Brochura de Estudo") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lessonInput,
                        onValueChange = { lessonInput = it },
                        label = { Text("Lição / Capítulo Atual") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = scheduleInput,
                        onValueChange = { scheduleInput = it },
                        label = { Text("Agenda de Estudo (Dia/Hora)") },
                        placeholder = { Text("Sábado às 14h") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notas de Progresso e Oração") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank()) {
                                    viewModel.addStudent(nameInput, bookInput, lessonInput, scheduleInput, notesInput)
                                }
                                showAddDialog = false
                            },
                            modifier = Modifier.testTag("student_dialog_save")
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentListItem(
    student: BibleStudent,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (student.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar",
                        tint = if (student.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = student.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (student.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (student.isActive) "Ativo" else "Inativo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (student.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = student.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.scale(0.7f).testTag("student_switch_${student.id}")
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Text(
                text = "📚 Estuda: ${student.bookOrTopic}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📖 Lição Atual: ${student.currentLesson}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (student.studySchedule.isNotBlank()) {
                        Text(
                            text = "⏰ Agenda: ${student.studySchedule}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }

            if (student.notes.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Notas: ${student.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PLACEMENTS SCREEN (REGISTO DE REVISTAS MARCADAS)
// -------------------------------------------------------------
@Composable
fun PlacementsScreen(viewModel: MinistryViewModel) {
    val placements by viewModel.placementsState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag("placements_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("placements_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registar Publicações")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Registo de Revistas e Publicações",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Grave os dados de pessoas que aceitaram revistas (A Sentinela/Despertai!) e outras publicações.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (placements.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ChromeReaderMode, contentDescription = "Empty", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Sem revistas registadas ainda.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(placements) { placement ->
                    PlacementListItem(
                        placement = placement,
                        onDelete = { viewModel.deletePlacement(placement) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            var personInput by remember { mutableStateOf("") }
            var addressInput by remember { mutableStateOf("") }
            var publicationsInput by remember { mutableStateOf("") }
            var notesInput by remember { mutableStateOf("") }

            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Registar Nova Distribuição", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = personInput,
                        onValueChange = { personInput = it },
                        label = { Text("Nome do Morador") },
                        modifier = Modifier.fillMaxWidth().testTag("placement_dialog_name")
                    )

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Endereço / Residência") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = publicationsInput,
                        onValueChange = { publicationsInput = it },
                        label = { Text("Publicações Marcadas (Ex: Sentinela Nº 3)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notas de Conversação / Assunto") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (personInput.isNotBlank()) {
                                    viewModel.addPlacement(personInput, addressInput, publicationsInput, notesInput)
                                }
                                showAddDialog = false
                            },
                            modifier = Modifier.testTag("placement_dialog_save")
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlacementListItem(
    placement: MagazinePlacement,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = placement.personName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (placement.address.isNotBlank()) {
                        Text(
                            text = "📍 ${placement.address}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            if (placement.publications.isNotBlank()) {
                Text(
                    text = "📚 Publicações: ${placement.publications}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (placement.notes.isNotBlank()) {
                Text(
                    text = "📝 Notas: ${placement.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// PROFILE SCREEN (PERFIL & CONFIGURAÇÕES DE CADASTRO)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MinistryViewModel) {
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var nameInput by remember { mutableStateOf(profile.name) }
    var roleInput by remember { mutableStateOf(profile.role) }
    var goalInput by remember { mutableStateOf(profile.monthlyGoalHours.toString()) }
    var dueInput by remember { mutableStateOf(profile.reportDueDay.toString()) }

    var rolesExpanded by remember { mutableStateOf(false) }
    val roles = listOf("Publicador", "Pioneiro Auxiliar", "Pioneiro Regular", "Pioneiro Especial")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Modalidade: ${profile.role}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            Text(
                text = "Cadastrar Informações do Perfil",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Nome Completo") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = rolesExpanded,
                onExpandedChange = { rolesExpanded = !rolesExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = roleInput,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Modalidade de Serviço") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rolesExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = rolesExpanded,
                    onDismissRequest = { rolesExpanded = false }
                ) {
                    roles.forEach { roleOption ->
                        DropdownMenuItem(
                            text = { Text(roleOption) },
                            onClick = {
                                roleInput = roleOption
                                goalInput = when(roleOption) {
                                    "Pioneiro Regular" -> "50"
                                    "Pioneiro Auxiliar" -> "30"
                                    "Publicador" -> "15"
                                    "Pioneiro Especial" -> "100"
                                    else -> "15"
                                }
                                rolesExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    label = { Text("Meta de Horas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dueInput,
                    onValueChange = { dueInput = it },
                    label = { Text("Dia Limite Entrega") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        val goal = goalInput.toIntOrNull() ?: 15
                        val due = dueInput.toIntOrNull() ?: 28
                        viewModel.updateProfile(nameInput.trim(), roleInput, goal, due)
                        Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "O nome não pode estar vazio.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Salvar Alterações", fontWeight = FontWeight.Bold)
            }
        }
    }
}
