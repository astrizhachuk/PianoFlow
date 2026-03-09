# Руководство по отображению музыкальной нотации в Jetpack Compose

Этот документ обобщает различные подходы к рендерингу музыкальной нотации в Android-приложении с использованием Jetpack Compose. Мы рассмотрим несколько методов, от простой ручной отрисовки до использования мощных JavaScript-библиотек, таких как VexFlow и Verovio.

## 1. Исходная проблема: Ручная отрисовка на Canvas

Изначальный подход заключался в ручной отрисовке нотного стана, ключей и нот с помощью `Canvas` в Jetpack Compose.

**Проблемы этого метода:**
*   **Хрупкое позиционирование:** Координаты нот задаются вручную через `Map`, что сложно поддерживать и расширять.
*   **Ограниченная нотация:** Реализована отрисовка только базовых овалов-нот, без штилей, длительностей, пауз и других символов.
*   **Неэффективный парсинг SVG:** Ручной разбор SVG-путей для ключей может быть неэффективным и не поддерживает все возможности формата.

---

## 2. Варианты решения и сравнение библиотек

Для решения этих проблем были рассмотрены три основных подхода, включая две популярные библиотеки: VexFlow и Verovio.

### Подход А: Парсинг MusicXML и собственный рендеринг

Это самый гибкий, но и самый сложный метод.

1.  **Идея:** Использовать стандартный формат `MusicXML` для описания партитуры.
2.  **Парсинг:** С помощью XML-парсера (например, `SimpleXML`) преобразовать MusicXML-файл в Kotlin-объекты (`Score`, `Measure`, `Note` и т.д.).
3.  **Рендеринг:** Написать собственный код, который будет "пробегать" по этим Kotlin-объектам и рисовать их на `Canvas` с помощью `drawCircle`, `drawLine` и т.д.

*   **Плюсы:** Максимальная производительность и полный контроль над внешним видом. Независимость от `WebView`.
*   **Минусы:** Очень высокая сложность реализации. Правильная расстановка нот (spacing) — нетривиальная задача.

### Подход Б: VexFlow (для интерактивности и генерации "на лету")

**VexFlow** — это "программистский" инструмент, идеальный для динамического создания нот и интерактивных упражнений.

1.  **Идея:** Интегрировать JavaScript-библиотеку VexFlow в приложение через `WebView`.
2.  **Реализация:**
    *   В `assets` создается HTML-файл, который подключает VexFlow.
    *   В HTML пишется JavaScript-функция (`renderNotes(notesJson)`), которая принимает данные о нотах в формате JSON.
    *   Из Kotlin-кода через `AndroidView` создается `WebView`, который загружает этот HTML.
    *   При обновлении данных (например, пользователь сыграл ноту) вызывается JS-функция с помощью `webView.evaluateJavascript(...)`.

*   **Плюсы:** Отличный контроль над каждым элементом. Идеален для создания нот "с нуля", подсветки и анимации. Проще в реализации, чем собственный рендерер.
*   **Минусы:** Требует использования `WebView`, что может быть медленнее, чем нативный `Canvas`.

### Подход В: Verovio (для отображения готовых партитур)

**Verovio** — это "музыковедческий" инструмент, созданный для отображения сложных, готовых партитур из стандартных форматов.

1.  **Идея:** Аналогично VexFlow, интегрируется через `WebView`.
2.  **Реализация:**
    *   Вместо JSON, Verovio "скармливается" целая строка в формате `MusicXML`.
    *   В `WebView` вызывается JavaScript-функция (`renderScore(musicXmlString)`), которая передает XML в Verovio.
    *   Verovio автоматически разбирает XML и отрисовывает всю партитуру согласно академическим стандартам.

*   **Плюсы:** Лучшее решение для отображения готовых файлов `.musicxml`. Автоматически обрабатывает верстку, переносы, сложные знаки. Имеет API для взаимодействия с отрисованными элементами (например, для получения их ID).
*   **Минусы:** Меньше контроля над внешним видом отдельных элементов по сравнению с VexFlow. Избыточен для отрисовки одной-двух нот.

---

## 3. Финальная цель и рекомендуемая архитектура

**Задача:** Создать приложение с двумя режимами:
1.  **Режим практики:** Загрузка готовой партитуры, проверка правильности игры пользователя и отображение ошибок.
2.  **Свободный режим:** Отображение нот, которые пользователь играет в реальном времени.

Для этой двойной цели **рекомендуется использовать только Verovio**, но с разной логикой подготовки данных для каждого режима.

### Единая архитектура на базе Verovio

1.  **"Глупый" UI-компонент (`PianoStaff`):**
    *   Создается Composable-компонент `PianoStaff`, который принимает на вход только готовую строку `musicXml: String`.
    *   Он ничего не знает о режимах работы и не занимается генерацией XML.
    *   Внутри `AndroidView` он передает эту строку в `WebView` для отрисовки с помощью Verovio.

2.  **"Умная" ViewModel (`PianoViewModel`):**
    *   `ViewModel` хранит текущий режим работы (`PRACTICE` или `LIVE`).
    *   Она отвечает за подготовку `musicXml` строки.
    *   **В режиме `PRACTICE`:**
        *   `ViewModel` загружает оригинальный MusicXML.
        *   При нажатии пользователем ноты она сравнивает ее с партитурой.
        *   Для отображения ошибки или правильного нажатия она **модифицирует оригинальную XML-строку** (например, добавляет атрибут `color` к ноте или временно вставляет "ноту-ошибку") и передает обновленный XML в `PianoStaff`.
    *   **В режиме `LIVE`:**
        *   `ViewModel` хранит список сыгранных пользователем нот.
        *   При каждом нажатии она **генерирует MusicXML "с нуля"** на основе этого списка и передает его в `PianoStaff`.

3.  **Связь:**
    *   UI (`PianoScreen`) подписывается на состояние `ViewModel`.
    *   При изменении `musicXml` в состоянии `ViewModel` компонент `PianoStaff` автоматически перерисовывается.

### Преимущества такой архитектуры

*   **Унификация:** Один движок рендеринга для всех задач.
*   **Разделение ответственности:** UI-компонент отвечает только за отображение, `ViewModel` — за логику.
*   **Масштабируемость:** Легко добавить новые режимы, не затрагивая UI.
*   **Гибкость:** Позволяет реализовать как отображение сложных партитур, так и мгновенную отрисовку нот, сыгранных пользователем.


### Ответ ИИ: Отображение ноты пользователя поверх партитуры

Да, отличный вопрос! Это ключевая функция для обучающего приложения, и Verovio, хоть и не напрямую, но позволяет это реализовать. Вы не сможете заставить Verovio "дорисовать" новую ноту поверх уже готовой партитуры, но можно добиться нужного эффекта, комбинируя возможности Verovio и WebView.

Идея состоит в том, чтобы динамически обновлять исходный MusicXML, добавляя в него "ноту пользователя" с особым оформлением (например, красным цветом), и заново перерисовывать фрагмент партитуры. Поскольку Verovio в WebView работает очень быстро, для пользователя это будет выглядеть как мгновенное появление "ноты-ошибки".

Вот как это можно реализовать, разделив на "что" и "как":

#### Что нужно сделать (концепция)

1.  **Загрузить и отобразить основную партитуру.** Это у вас уже работает.
2.  **Держать в памяти (в ViewModel) модель партитуры.** Это может быть либо оригинальная XML-строка, либо, что гораздо лучше, распарсенный Kotlin-объект.
3.  **При нажатии пользователем ноты:**
    *   Вы получаете MIDI-сигнал.
    *   Проверяете, совпадает ли он с ожидаемой нотой в партитуре.
    *   Если не совпадает (ошибка):
        *   Вы программно добавляете информацию об этой ошибочной ноте в вашу модель данных (например, в тот же такт, где находится ожидаемая нота).
        *   Вы генерируете новый MusicXML на основе этой измененной модели. В этом XML вы можете пометить ошибочную ноту специальным образом, например, добавить ей атрибут цвета.
        *   Вы передаете этот новый, обновленный MusicXML в WebView и даете команду Verovio полностью перерисовать SVG.
4.  **Стилизация в Verovio:** MusicXML поддерживает указание цвета для нот. Verovio это учтет и отрисует ноту в заданном цвете.
5.  **Возврат к исходному состоянию:** Когда пользователь нажимает правильную ноту или по прошествии времени, вы убираете "ноту-ошибку" из модели и снова перерисовываете стан в исходном виде.

#### Как это реализовать (практические шаги)

Давайте модифицируем ваш существующий код.

##### 1. Улучшаем `generateNoteXML` и `generateMusicXML`

Нам нужно передавать в эти функции больше информации: не только список "правильных" нот, но и опциональную "ноту ошибки", которую сыграл пользователь.
Изменим `generateNoteXML`, чтобы он мог добавлять цвет:

```kotlin
/**
 * Генерирует XML для одной ноты с возможностью указания цвета.
 */
private fun generateNoteXML(note: Note, duration: Int = 4, isChord: Boolean = false, color: String? = null): String {
    val (step, alter, octave) = pitchToStepOctave(note.pitch) // Модифицируем pitchToStepOctave, чтобы она возвращала и alter

    val colorAttribute = if (color != null) " color=\"$color\"" else ""

    // XML-представление знака альтерации (диез)
    val alterXml = if (alter > 0) "<alter>$alter</alter>" else ""

    return '''
    <note$colorAttribute>
        ${if (isChord) "<chord/>" else ""}
        <pitch>
            <step>$step</step>
            $alterXml
            <octave>$octave</octave>
        </pitch>
        <duration>$duration</duration>
        <voice>1</voice>
        <type>quarter</type>
        <stem>up</stem>
    </note>
    '''
}
```

Модифицируйте `pitchToStepOctave`, чтобы она возвращала `Triple` (Step, Alter, Octave):

```kotlin
/**
 * Конвертирует MIDI pitch в шаг (step), альтерацию (alter) и октаву.
 * pitch 60 = C4 (Middle C)
 */
private fun pitchToStepOctave(pitch: Int): Triple<String, Int, Int> {
    val steps = arrayOf("C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B")
    val alterations = arrayOf(0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0) // 0 = нет, 1 = диез

    val noteInOctave = pitch % 12
    val octave = (pitch / 12) - 1
    val step = steps[noteInOctave]
    val alter = alterations[noteInOctave]

    return Triple(step, alter, octave)
}
```

Теперь `generateMusicXML` должна принимать `playedNote`:

```kotlin
private fun generateMusicXML(originalNotes: List<Note>, playedNote: Note? = null): String {
    // ... (начало функции как у вас)

    val allNotes = originalNotes.toMutableList()
    var playedNoteIsError = false

    if (playedNote != null) {
        // Проверяем, есть ли сыгранная нота среди оригинальных
        val isCorrect = originalNotes.any { it.pitch == playedNote.pitch }
        if (!isCorrect) {
            allNotes.add(playedNote)
            playedNoteIsError = true
        }
    }
    
    // Сортируем ноты по высоте для корректного отображения аккорда
    allNotes.sortBy { it.pitch }

    // ... (далее генерация тактов)
    measuresXML.append("    <measure number=\"1\">\n")
    // ... (атрибуты)

    allNotes.forEachIndexed { index, note ->
        val isChord = index > 0
        var color: String? = null

        if (playedNoteIsError && note.pitch == playedNote?.pitch) {
            color = "#FF0000" // Красный цвет для ошибки
        }

        measuresXML.append(generateNoteXML(note, duration = 4, isChord = isChord, color = color))
    }

    measuresXML.append("    </measure>\n")

    // ... (конец функции)

    // ... возвращаем полный XML
}
```

##### 2. PianoStaff

```kotlin
@Composable
fun PianoStaff(
    notes: List<Note>,
    playedNote: Note? = null, // Добавляем новое состояние
    modifier: Modifier = Modifier
    // ...
) {
    Box(modifier = modifier) {
        // Генерируем MusicXML с учетом сыгранной ноты
        val musicXML = generateMusicXML(notes, playedNote)
        val escapedXML = musicXML.replace(...) // Как у вас

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    // ... все ваши настройки ...
                    
                    // HTML-шаблон теперь будет содержать функции обновления
                    val htmlContent = getHtmlTemplate() // Выносим шаблон в отдельную функцию
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                // Эта магия будет происходить при каждой рекомпозиции!
                // Когда `playedNote` изменится, Compose вызовет этот блок.
                val newMusicXml = generateMusicXML(notes, playedNote)
                val newEscapedXml = newMusicXml.replace(...) // Экранирование

                // Вызываем JS-функцию для обновления данных и перерисовки
                webView.evaluateJavascript("updateAndRender(\'$newEscapedXml\')", null)
            }
        )
    }
}
```

##### 3. Обновляем HTML и JavaScript

Нам нужен способ обновлять SVG без перезагрузки всей страницы. Вынесите ваш HTML-шаблон в отдельную функцию `getHtmlTemplate()` и добавьте в него функцию `updateAndRender`:

```html
<!-- Внутри тега <script> в вашем HTML -->

<script>
var vrvToolkit = null;

function renderNotation(musicXml) {
    // ... (ваш существующий код для первой отрисовки) ...
    vrvToolkit.loadData(musicXml);
    // ...
    var svg = vrvToolkit.renderToSVG(1);
    document.getElementById('notation').innerHTML = svg;
}

// НОВАЯ ФУНКЦИЯ, вызываемая из Kotlin
function updateAndRender(newMusicXml) {
    if (!vrvToolkit) {
        // Если Toolkit еще не инициализирован, делаем полную отрисовку
        renderNotation(newMusicXml);
        return;
    }
    try {
        // Загружаем новые данные и перерисовываем
        vrvToolkit.loadData(newMusicXml);
        var svg = vrvToolkit.renderToSVG(1);
        document.getElementById('notation').innerHTML = svg;
    } catch (e) {
        console.error('Update error:', e);
        document.getElementById('error').innerHTML = 'Update Error: ' + e.message;
    }
}

// При первоначальной загрузке вызываем renderNotation
window.addEventListener('load', function() {
    // При первой загрузке можно передать пустой стан
    renderNotation(getEmptyMusicXML()); // getEmptyMusicXML() должна быть доступна в JS
});
</script>
```

Вместо `renderNotation` при первой загрузке вы можете передавать начальный XML из Kotlin, как и делаете сейчас. Главное — это наличие функции `updateAndRender` для последующих обновлений.

**Итог:**

С этим подходом вы не просто "подсвечиваете" ноту, а показываете пользователю реальную картину: вот ноты, которые нужно было нажать, а вот та (красная), которую ты нажал по ошибке. Это гораздо более информативно для обучения.

**Ключевые моменты для успеха:**

1.  Состояние (`playedNote`) управляется извне `PianoStaff` (вероятно, из `ViewModel`).
2.  При изменении состояния Jetpack Compose вызывает блок `update` у `AndroidView`.
3.  Блок `update` вызывает JavaScript-функцию `updateAndRender`, передавая ей полностью новый MusicXML.
4.  Verovio перерисовывает SVG, и пользователь видит результат.

### Два режима

Отличная идея! Наличие двух режимов — это именно то, что сделает ваше приложение PianoFlow по-настоящему гибким и полезным. Один режим для разучивания готовых произведений, второй — для свободного творчества или анализа своей игры.

Учитывая эту двойную цель, вам не нужно выбирать что-то одно. Вам нужно использовать оба подхода, но с умной архитектурой, чтобы они не конфликтовали, а дополняли друг друга.

*   **Verovio** станет вашим основным движком для отображения партитур.
*   **VexFlow** может стать легковесной альтернативой для "свободного режима", если Verovio окажется слишком медленным для мгновенного отображения одиночных нот (что маловероятно, но возможно).

Однако, самый элегантный и поддерживаемый подход — использовать **только Verovio для обоих режимов**, но с разной логикой подготовки данных. Это унифицирует ваш код отрисовки и упростит поддержку.

#### Единая архитектура на базе Verovio для двух режимов

Вот как можно построить систему, используя Verovio для обеих задач, прямо в вашем файле `PianoStaff.kt`.

##### 1. Модификация `PianoStaff` для максимальной гибкости

Ваш Composable `PianoStaff` должен перестать быть "умным" (то есть перестать сам генерировать XML). Вместо этого он должен стать "глупым" компонентом, который просто принимает готовую XML-строку и умеет обновляться.

```kotlin
@Composable
fun PianoStaff(
    // Принимает уже готовую строку. Кто её сгенерировал - не его забота.
    musicXml: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Мы используем musicXml как ключ. Если он изменится,
        // Compose поймет, что нужно пересоздать или обновить AndroidView.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                // ... создание и базовая настройка WebView ...
                WebView(context).apply {
                    // ... настройки ...
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                // Эта функция будет вызываться при первой загрузке И при каждом обновлении `musicXml`.
                val escapedXML = musicXml.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")

                // HTML-шаблон лучше вынести в отдельную функцию или строковый ресурс
                val htmlContent = getWebViewContent(escapedXML)
                webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
            }
        )
    }
}

// Эту функцию нужно будет доработать для обновления без перезагрузки
private fun getWebViewContent(escapedXML: String): String {
    // Весь ваш HTML-код из текущей реализации идет сюда.
    // Переменная `musicXml` в JavaScript теперь будет `"$escapedXML"`.
    return '''
    <!DOCTYPE html>
    <html>
    <!-- ... ваш HTML ... -->
    <script>
        var musicXml = "$escapedXML";
        // ... ваш JavaScript ...
    </script>
    </html>
    '''.trimIndent()
}
```

Теперь ваш `PianoStaff` готов к любым задачам.

##### 2. Реализация двух режимов на уровне ViewModel

Вся логика теперь будет жить в `ViewModel`. `ViewModel` будет решать, какой MusicXML сгенерировать и передать в `PianoStaff`.

```kotlin
// Пример ViewModel
class PianoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PianoUiState())
    val uiState: StateFlow<PianoUiState> = _uiState.asStateFlow()

    // --- Режим 1: Практика по готовой партитуре ---

    fun loadScoreForPractice(scoreXml: String) {
        _uiState.update {
            it.copy(
                mode = AppMode.PRACTICE,
                // Отображаем оригинальную партитуру
                renderedXml = scoreXml,
                // Храним оригинал для логики проверки
                originalScoreXml = scoreXml
                // Здесь же можно распарсить XML в Kotlin-объекты для удобства
            )
        }
    }

    fun onNotePlayedInPractice(playedNote: Note) {
        val currentState = _uiState.value
        val originalXml = currentState.originalScoreXml ?: return

        // 1. Проверяем, правильная ли нота.
        // val isCorrect = checkNoteAgainstScore(playedNote, originalXml)
        val isCorrect = true // Упрощенная логика

        // 2. Генерируем новый XML с подсветкой или "нотой ошибки".
        val updatedXml = if (isCorrect) {
            // Подсвечиваем правильную ноту в `originalXml`
            highlightNoteInXml(originalXml, playedNote.pitch, "green")
        } else {
            // Добавляем красную "ноту ошибки" в `originalXml`
            addErrorNoteToXml(originalXml, playedNote, "red")
        }

        // 3. Обновляем UI
        _uiState.update { it.copy(renderedXml = updatedXml) }

        // 4. Через секунду можно вернуть исходный вид
        viewModelScope.launch {
            delay(1000)
            _uiState.update { it.copy(renderedXml = originalXml) }
        }
    }


    // --- Режим 2: Свободная игра ("Live" режим) ---

    fun enterLiveMode() {
        _uiState.update {
            it.copy(
                mode = AppMode.LIVE,
                livePlayedNotes = emptyList(),
                renderedXml = getEmptyMusicXML() // Показываем пустой стан
            )
        }
    }

    fun onNotePlayedInLive(playedNote: Note) {
        val currentNotes = _uiState.value.livePlayedNotes.toMutableList()
        currentNotes.add(playedNote)

        // Генерируем MusicXML "на лету" из сыгранных нот
        val liveXml = generateMusicXML(currentNotes)

        _uiState.update {
            it.copy(
                livePlayedNotes = currentNotes,
                renderedXml = liveXml
            )
        }
    }
}

// Модель состояния UI
data class PianoUiState(
    val mode: AppMode = AppMode.PRACTICE,
    val renderedXml: String = getEmptyMusicXML(),
    val originalScoreXml: String? = null,
    val livePlayedNotes: List<Note> = emptyList()
)

enum class AppMode { PRACTICE, LIVE }

// Вспомогательные функции (заглушки, требуют реализации)
private fun highlightNoteInXml(xml: String, pitch: Int, color: String): String = xml
private fun addErrorNoteToXml(xml: String, note: Note, color: String): String = xml
```

##### 3. Собираем все вместе на экране

```kotlin
@Composable
fun PianoScreen(viewModel: PianoViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column {
        // Кнопки для переключения режимов
        Row {
            Button(onClick = { viewModel.loadScoreForPractice(somePreloadedScore) }) {
                Text("Practice Mode")
            }
            Button(onClick = { viewModel.enterLiveMode() }) {
                Text("Live Mode")
            }
        }

        // Наш универсальный компонент отображения
        PianoStaff(
            musicXml = uiState.renderedXml,
            modifier = Modifier.weight(1f)
        )

        // Ваш компонент пианино, который вызывает onNotePlayedIn...
        PianoKeyboard(
            onNoteOn = { note ->
                if (uiState.mode == AppMode.PRACTICE) {
                    viewModel.onNotePlayedInPractice(note)
                } else {
                    viewModel.onNotePlayedInLive(note)
                }
            },
            // ...
        )
    }
}
```

**Преимущества этого подхода:**

1.  **Единый компонент отрисовки:** У вас есть один `PianoStaff`, который ничего не знает о режимах. Он просто рисует то, что ему дают. Это упрощает его тестирование и поддержку.
2.  **Централизованная логика:** Вся логика генерации и обновления XML находится в `ViewModel`. Это правильное разделение ответственности (Separation of Concerns).
3.  **Гибкость:** Вы можете легко добавить третий режим, просто добавив новую логику в `ViewModel`, не трогая UI-компоненты.
4.  **Сохранение вашего кода:** Вы продолжите использовать ваши функции `generateMusicXML`, `generateNoteXML` и т.д. для "Live" режима. Для "Practice" режима вам понадобятся новые функции, которые будут не генерировать XML с нуля, а модифицировать существующий (например, с помощью строковых операций или XML-парсера).

**Вывод:** Используйте Verovio для обоих режимов. Сделайте ваш `PianoStaff` "глупым" компонентом, который принимает готовую XML-строку. Всю логику по подготовке этой строки для каждого из режимов вынесите в `ViewModel`. Это наиболее чистое, масштабируемое и профессиональное решение для вашей задачи.

---

## 4. Анализ производительности с помощью System Trace

Профилировщик Android Studio может быть перегружен информацией. Чтобы точечно измерить производительность ключевых участков кода, можно использовать кастомные метки трассировки (`androidx.tracing.ktx.trace`).

### Как найти добавленные метки в Profiler

1.  **Выберите режим "System Trace"**: В окне Profiler на дорожке CPU выберите конфигурацию записи **System Trace**. Это критически важный шаг.
2.  **Начните запись**: Нажмите кнопку **Record** и повзаимодействуйте с приложением несколько секунд.
3.  **Остановите запись**.
4.  **Используйте поиск**: После остановки записи найдите **строку поиска** (обычно справа вверху) и введите имя метки, которую вы хотите найти (например, `PianoStaff:WebView:update`).
5.  **Анализ**: Профайлер автоматически отфильтрует все события и подсветит на временной шкале только те блоки, которые вы искали. Вы сможете увидеть их точную длительность и частоту вызовов.

### Полезные ссылки на официальную документацию

*   **Основная документация по System Trace**: [https://developer.android.com/topic/performance/tracing](https://developer.android.com/topic/performance/tracing)
    *   *Объясняет, как записывать и анализировать системные трейсы.*
*   **Документация по кастомным меткам (Custom Events)**: [https://developer.android.com/topic/performance/tracing/custom-events](https://developer.android.com/topic/performance/tracing/custom-events)
    *   *Описывает использование блока `trace {}`, который был добавлен в код.*
*   **Обзор CPU Profiler**: [https://developer.android.com/studio/profile/cpu-profiler](https://developer.android.com/studio/profile/cpu-profiler)
    *   *Общая информация об инструменте.*


Чего не хватает согласно стратегии:
1.
UI-тесты для основного функционала (Piano Staff):
◦
В стратегии указано: "UI correctness: Checking that the UI correctly reacts to state changes coming from the ViewModel".
◦
Отсутствует: androidTest для PianoStaff. Нужно проверить, что при получении нот через ViewModel они корректно отображаются на нотном стане.
2.
Интеграционные UI-тесты анализа аккордов:
◦
Есть unit-тесты логики анализа, но нет тестов, подтверждающих, что результат анализа (название аккорда) появляется на экране пользователя.
3.
End-to-End сценарии (User Flows):
◦
Стратегия предполагает проверку цепочек действий: "connected the device -> a notification appeared on the screen".
◦
Не хватает: Теста, который симулирует поток MIDI-сообщений (через Fake) и проверяет обновление всей цепочки: MidiDataSource -> Repository -> UseCase -> ViewModel -> UI (PianoStaff + Chord Name).
4.
Реализация вспомогательных классов для тестов:
◦
В документе упоминается VirtualMidiDeviceHelper для эмуляции на уровне системы. Сейчас в MidiConnectionUITest используется FakeMidiRepository, что проще, но менее "глубоко", чем описано в стратегии для полной эмуляции.
Рекомендации по доработке:
1.
Создать PianoStaffUITest.kt в androidTest, чтобы проверить визуализацию нот.
2.
Добавить комплексный тест в MidiConnectionUITest или новый файл, который проверит отображение названия аккорда при "нажатии" клавиш на фейковом устройстве.
3.
Проверить наличие FakeMidiDataSource (как указано в примере стратегии) для более гибкого тестирования инструментальных сценариев без замены целого репозитория.