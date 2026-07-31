package br.com.mwconsulting.championcompanion.feature.calendar

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.model.CalendarDayPresence
import br.com.mwconsulting.championcompanion.core.model.CalendarMonthSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(
    onOpenDate: (LocalDate) -> Unit = {},
    calendarViewModel: CalendarViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) { ViewModelProvider(activity)[CalendarViewModel::class.java] }
    },
) {
    val state by calendarViewModel.state.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now(state.zoneId)

    LaunchedEffect(calendarViewModel) {
        calendarViewModel.onIntent(CalendarIntent.Load)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            CalendarHeader()
            MonthHeader(
                month = state.selectedMonth,
                locale = locale,
                onPrevious = { calendarViewModel.onIntent(CalendarIntent.PreviousMonth) },
                onNext = { calendarViewModel.onIntent(CalendarIntent.NextMonth) },
                onToday = { calendarViewModel.onIntent(CalendarIntent.GoToToday) },
            )

            when {
                state.monthSnapshot == null && state.error == null -> LoadingState(
                    label = "Carregando calendário",
                    message = "Consultando dados locais",
                    modifier = Modifier.fillMaxWidth(),
                )

                state.monthSnapshot == null && state.error != null -> ErrorState(
                    title = "Calendário indisponível",
                    message = state.error.toCalendarMessageOrFallback(),
                    actionLabel = "Tentar novamente",
                    onAction = { calendarViewModel.onIntent(CalendarIntent.Retry) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val monthSnapshot = state.monthSnapshot
            val monthError = state.error?.takeIf { it.scope == CalendarUiErrorScope.MONTH }
            if (monthSnapshot != null && (monthSnapshot.issues.isNotEmpty() || monthError != null)) {
                val hasMonthIssues = monthSnapshot.issues.isNotEmpty()
                ErrorState(
                    title = if (hasMonthIssues) "Dados parciais do mês" else "Dados do mês indisponíveis",
                    message = if (hasMonthIssues) {
                        "Parte dos dados locais não pôde ser consultada. Dias sem indicadores permanecem sem dados."
                    } else {
                        monthError?.toCalendarMessage()
                            ?: "Não foi possível carregar dados locais. Tente novamente."
                    },
                    actionLabel = if (hasMonthIssues || monthError?.isRetryable != false) "Tentar novamente" else null,
                    onAction = if (hasMonthIssues || monthError?.isRetryable != false) {
                        { calendarViewModel.onIntent(CalendarIntent.Load) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            monthSnapshot?.let { snapshot ->
                MonthCalendar(
                    month = state.selectedMonth,
                    selectedDate = state.selectedDate,
                    today = today,
                    snapshot = snapshot,
                    locale = locale,
                    onSelectDate = { calendarViewModel.onIntent(CalendarIntent.SelectDate(it)) },
                )
                CalendarLegend()
                if (snapshot.days.isEmpty() && snapshot.issues.isEmpty()) {
                    EmptyState(
                        title = "Mês sem registros",
                        message = "Nenhum indicador local foi encontrado neste mês.",
                    )
                }
                SelectedDateActions(
                    selectedDate = state.selectedDate,
                    locale = locale,
                    dayLoading = state.dayLoading,
                    hasDayDetail = state.dayDetail != null,
                    dayError = state.error?.takeIf { it.scope == CalendarUiErrorScope.DAY },
                    onOpenDate = onOpenDate,
                    onRetry = { calendarViewModel.onIntent(CalendarIntent.Retry) },
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Calendário",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Histórico local por dia",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    locale: Locale,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Mês exibido: ${formatMonth(month, locale)}"
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = formatMonth(month, locale),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.semantics { contentDescription = "Mês anterior" },
                ) {
                    Text("‹", style = MaterialTheme.typography.headlineMedium)
                }
                Button(onClick = onToday) { Text("Hoje") }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.semantics { contentDescription = "Próximo mês" },
                ) {
                    Text("›", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    snapshot: CalendarMonthSnapshot,
    locale: Locale,
    onSelectDate: (LocalDate) -> Unit,
) {
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val weekdays = (0..6).map { firstDay.plus(it.toLong()) }
    val leadingEmptyCells = month.atDay(1).dayOfWeek.daysFrom(firstDay)
    val dates = (0 until 42).map { index ->
        val dayOffset = index - leadingEmptyCells
        month.atDay(1).plusDays(dayOffset.toLong()).takeIf { YearMonth.from(it) == month }
    }
    val presenceByDate = snapshot.days.associateBy { it.localDate }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(
                horizontal = ChampionSpacing.small,
                vertical = ChampionSpacing.small,
            ),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
            ) {
                weekdays.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                            },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            dates.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            EmptyCalendarCell()
                        } else {
                            CalendarDayCell(
                                date = date,
                                presence = presenceByDate[date],
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                locale = locale,
                                onClick = { onSelectDate(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.EmptyCalendarCell() {
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp),
    )
}

@Composable
private fun RowScope.CalendarDayCell(
    date: LocalDate,
    presence: CalendarDayPresence?,
    isToday: Boolean,
    isSelected: Boolean,
    locale: Locale,
    onClick: () -> Unit,
) {
    val cellDescription = date.toCalendarDescription(
        locale = locale,
        presence = presence,
        isToday = isToday,
        isSelected = isSelected,
    )
    val background = when {
        presence?.hasSyncFailure == true -> MaterialTheme.colorScheme.errorContainer
        presence?.hasSyncPending == true -> MaterialTheme.colorScheme.tertiaryContainer
        (presence?.workoutCount ?: 0) > 0 -> MaterialTheme.colorScheme.primaryContainer
        presence?.hasHeartRate == true -> MaterialTheme.colorScheme.secondaryContainer
        presence?.hasSteps == true -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val cellContentColor = calendarCellContentColor(presence)
    val borderColor = when {
        isToday -> cellContentColor
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val borderWidth = when {
        isToday -> 2.dp
        isSelected -> 3.dp
        else -> 0.dp
    }
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = cellDescription
                role = Role.Button
                selected = isSelected
                stateDescription = if (isSelected) "Selecionado" else "Não selecionado"
            },
        shape = RoundedCornerShape(10.dp),
        color = background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ChampionSpacing.extraSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = cellContentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            )
            CalendarPresenceMarkers(presence, background)
        }
    }
}

@Composable
private fun CalendarPresenceMarkers(presence: CalendarDayPresence?, background: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (presence?.hasSteps == true) {
            CalendarPresenceDot(calendarStepsColor(background))
        }
        if (presence?.hasHeartRate == true) {
            CalendarPresenceDot(calendarHeartRateColor(background))
        }
        if ((presence?.workoutCount ?: 0) > 0) {
            CalendarPresenceDot(calendarActivityColor(background))
        }
    }
}

@Composable
private fun CalendarPresenceDot(color: Color) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .background(color, CircleShape),
    )
}

private fun calendarStepsColor(background: Color): Color = if (background.luminance() > 0.5f) {
    Color(0xFF1565C0)
} else {
    Color(0xFF64B5F6)
}

private fun calendarHeartRateColor(background: Color): Color = if (background.luminance() > 0.5f) {
    Color(0xFF2E7D32)
} else {
    Color(0xFF81C784)
}

private fun calendarActivityColor(background: Color): Color = if (background.luminance() > 0.5f) {
    Color(0xFF8A6D00)
} else {
    Color(0xFFFFD54F)
}

@Composable
private fun calendarCellContentColor(presence: CalendarDayPresence?): Color = when {
    presence?.hasSyncFailure == true -> MaterialTheme.colorScheme.onErrorContainer
    presence?.hasSyncPending == true -> MaterialTheme.colorScheme.onTertiaryContainer
    (presence?.workoutCount ?: 0) > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
    presence?.hasHeartRate == true -> MaterialTheme.colorScheme.onSecondaryContainer
    presence?.hasSteps == true -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun CalendarLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Text("Legenda", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val legendBackground = MaterialTheme.colorScheme.surfaceContainerLow
            CalendarLegendItem("Passos", calendarStepsColor(legendBackground))
            CalendarLegendItem("Frequência cardíaca", calendarHeartRateColor(legendBackground))
            CalendarLegendItem("Atividade física", calendarActivityColor(legendBackground))
            CalendarLegendStateItem("Falha de sincronização", MaterialTheme.colorScheme.errorContainer)
            CalendarLegendStateItem("Sincronização pendente", MaterialTheme.colorScheme.tertiaryContainer)
        }
    }
}

@Composable
private fun CalendarLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
    ) {
        CalendarPresenceDot(color)
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CalendarLegendStateItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SelectedDateActions(
    selectedDate: LocalDate?,
    locale: Locale,
    dayLoading: Boolean,
    hasDayDetail: Boolean,
    dayError: CalendarUiError?,
    onOpenDate: (LocalDate) -> Unit,
    onRetry: () -> Unit,
) {
    selectedDate?.let { date ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChampionSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                Text(
                    "Dia selecionado: ${formatDate(date, locale)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    when {
                        dayLoading -> "Carregando dados locais do dia"
                        dayError != null -> "Não foi possível carregar dados locais do dia"
                        hasDayDetail -> "Dados locais do dia carregados"
                        else -> "Selecione abrir para consultar o resumo do dia"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                dayError?.let { error ->
                    ErrorState(
                        title = "Dados do dia indisponíveis",
                        message = error.toCalendarMessage(),
                        actionLabel = if (error.isRetryable) "Tentar novamente" else null,
                        onAction = if (error.isRetryable) onRetry else null,
                    )
                }
                Button(
                    onClick = { onOpenDate(date) },
                    enabled = !dayLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (dayLoading) "Carregando…" else "Abrir dia")
                }
            }
        }
    }
}

private fun DayOfWeek.daysFrom(firstDay: DayOfWeek): Int =
    (ordinal - firstDay.ordinal + 7) % 7

private fun formatMonth(month: YearMonth, locale: Locale): String =
    month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.uppercase(locale) }

private fun formatDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(locale))

private fun LocalDate.toCalendarDescription(
    locale: Locale,
    presence: CalendarDayPresence?,
    isToday: Boolean,
    isSelected: Boolean,
): String = buildList {
    add(formatDate(this@toCalendarDescription, locale))
    if (isToday) add("hoje")
    if (isSelected) add("selecionado")
    if (presence?.hasSteps == true) add("passos registrados")
    if (presence?.hasHeartRate == true) add("BPM registrado")
    if ((presence?.workoutCount ?: 0) > 0) {
        val count = presence?.workoutCount ?: 0
        add(if (count == 1) "1 atividade" else "$count atividades")
    }
    if (presence?.hasWeight == true) add("peso registrado")
    if (presence?.hasSyncPending == true) add("sincronização pendente")
    if (presence?.hasSyncFailure == true) add("erro de sincronização")
    if (presence == null) add("sem dados locais")
    if (!isSelected) add("toque para selecionar")
}.joinToString(", ")

private fun CalendarUiError?.toCalendarMessageOrFallback(): String = when (this?.category) {
    CalendarUiErrorCategory.INVALID_DATE -> "A data selecionada não pertence ao mês exibido."
    CalendarUiErrorCategory.INVALID_LOCAL_DATA -> "Há dados locais inválidos para este mês."
    CalendarUiErrorCategory.SOURCE_UNAVAILABLE,
    CalendarUiErrorCategory.INTERNAL,
    null,
    -> "Não foi possível carregar dados locais. Tente novamente."
}

private fun CalendarUiError.toCalendarMessage(): String = when (category) {
    CalendarUiErrorCategory.INVALID_DATE -> "A data selecionada não pertence ao mês exibido."
    CalendarUiErrorCategory.INVALID_LOCAL_DATA -> "Há dados locais inválidos para este mês."
    CalendarUiErrorCategory.SOURCE_UNAVAILABLE,
    CalendarUiErrorCategory.INTERNAL,
    -> "Não foi possível carregar dados locais. Tente novamente."
}
