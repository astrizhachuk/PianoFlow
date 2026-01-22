package com.astrizhachuk.pianoflow.presentation.model.pianostaff

/**
 * Представляет состояние пользовательского интерфейса для экрана с нотным станом.
 *
 * Этот data-класс содержит музыкальные ноты для скрипичного и басового ключей,
 * сериализованные в виде JSON-строк. Этот формат используется для эффективной передачи
 * данных о нотах в UI-слой, как правило, для отрисовки в WebView или аналогичном компоненте.
 *
 * @param trebleNotesJson JSON-строка, представляющая массив нот для скрипичного ключа.
 * @param bassNotesJson JSON-строка, представляющая массив нот для басового ключа.
 */
data class PianoStaffUiState(
    val trebleNotesJson: String = "[]",
    val bassNotesJson: String = "[]"
)
