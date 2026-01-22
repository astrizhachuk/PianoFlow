# A guide to displaying musical notation in Jetpack Compose

This document summarizes various approaches to rendering musical notation in an Android application using Jetpack Compose. We'll look at several methods, from simple manual drawing to using powerful JavaScript libraries like VexFlow and Verovio.

## 1. The original problem: Manual drawing on a Canvas

The initial approach was to manually draw the staff, clefs, and notes using `Canvas` in Jetpack Compose.

**Problems with this method:**
*   **Fragile positioning:** The coordinates of the notes are set manually via a `Map`, which is difficult to maintain and expand.
*   **Limited notation:** Only the drawing of basic oval notes is implemented, without stems, durations, rests, and other symbols.
*   **Inefficient SVG parsing:** Manual parsing of SVG paths for keys can be inefficient and does not support all the features of the format.

---

## 2. Solution options and library comparison

To solve these problems, three main approaches were considered, including two popular libraries: VexFlow and Verovio.

### Approach A: Parsing MusicXML and custom rendering

This is the most flexible, but also the most complex method.

1.  **Idea:** Use the standard `MusicXML` format to describe the score.
2.  **Parsing:** Using an XML parser (for example, `SimpleXML`), convert the MusicXML file into Kotlin objects (`Score`, `Measure`, `Note`, etc.).
3.  **Rendering:** Write your own code that will "run" through these Kotlin objects and draw them on a `Canvas` using `drawCircle`, `drawLine`, etc.

*   **Pros:** Maximum performance and full control over the appearance. Independence from `WebView`.
*   **Cons:** Very high implementation complexity. Correct note spacing is a non-trivial task.

### Approach B: VexFlow (for interactivity and on-the-fly generation)

**VexFlow** is a "programmer's" tool, ideal for dynamically creating notes and interactive exercises.

1.  **Idea:** Integrate the VexFlow JavaScript library into the application via `WebView`.
2.  **Implementation:**
    *   An HTML file is created in `assets` that connects VexFlow.
    *   A JavaScript function (`renderNotes(notesJson)`) is written in HTML that accepts note data in JSON format.
    *   A `WebView` is created from Kotlin code via `AndroidView` that loads this HTML.
    *   When the data is updated (for example, the user has played a note), the JS function is called using `webView.evaluateJavascript(...)`.

*   **Pros:** Excellent control over each element. Ideal for creating notes "from scratch", highlighting, and animation. Easier to implement than a custom renderer.
*   **Cons:** Requires the use of `WebView`, which can be slower than a native `Canvas`.

### Approach C: Verovio (for displaying ready-made scores)

**Verovio** is a "musicologist's" tool created for displaying complex, ready-made scores from standard formats.

1.  **Idea:** Similar to VexFlow, it is integrated via `WebView`.
2.  **Implementation:**
    *   Instead of JSON, Verovio is "fed" a whole string in `MusicXML` format.
    *   A JavaScript function (`renderScore(musicXmlString)`) is called in `WebView`, which passes the XML to Verovio.
    *   Verovio automatically parses the XML and draws the entire score according to academic standards.

*   **Pros:** The best solution for displaying ready-made `.musicxml` files. Automatically handles layout, line breaks, and complex signs. It has an API for interacting with the rendered elements (for example, to get their ID).
*   **Cons:** Less control over the appearance of individual elements compared to VexFlow. It is redundant for drawing one or two notes.

---

## 3. The final goal and recommended architecture

**Task:** Create an application with two modes:
1.  **Practice mode:** Loading a ready-made score, checking the correctness of the user's playing, and displaying errors.
2.  **Free mode:** Displaying the notes that the user is playing in real time.

For this dual purpose, **it is recommended to use only Verovio**, but with different data preparation logic for each mode.

### A single architecture based on Verovio

1.  **"Dumb" UI component (`PianoStaff`):**
    *   A `PianoStaff` Composable component is created that accepts only a ready-made `musicXml: String` string as input.
    *   It knows nothing about the operating modes and does not generate XML.
    *   Inside `AndroidView`, it passes this string to `WebView` for rendering using Verovio.

2.  **"Smart" ViewModel (`PianoViewModel`):**
    *   The `ViewModel` stores the current operating mode (`PRACTICE` or `LIVE`).
    *   It is responsible for preparing the `musicXml` string.
    *   **In `PRACTICE` mode:**
        *   The `ViewModel` loads the original MusicXML.
        *   When the user presses a note, it compares it with the score.
        *   To display an error or a correct press, it **modifies the original XML string** (for example, adds a `color` attribute to the note or temporarily inserts an "error note") and passes the updated XML to `PianoStaff`.
    *   **In `LIVE` mode:**
        *   The `ViewModel` stores a list of the notes played by the user.
        *   With each press, it **generates MusicXML "from scratch"** based on this list and passes it to `PianoStaff`.

3.  **Connection:**
    *   The UI (`PianoScreen`) subscribes to the state of the `ViewModel`.
    *   When `musicXml` changes in the `ViewModel` state, the `PianoStaff` component is automatically redrawn.

### Advantages of this architecture

*   **Unification:** One rendering engine for all tasks.
*   **Separation of responsibilities:** The UI component is responsible only for display, the `ViewModel` is responsible for logic.
*   **Scalability:** It is easy to add new modes without affecting the UI.
*   **Flexibility:** It allows you to implement both the display of complex scores and the instant drawing of notes played by the user.


### AI response: Displaying the user's note on top of the score

Yes, great question! This is a key feature for a learning application, and Verovio, although not directly, allows you to implement it. You will not be able to force Verovio to "draw" a new note on top of an already finished score, but you can achieve the desired effect by combining the capabilities of Verovio and WebView.

The idea is to dynamically update the source MusicXML by adding a "user's note" to it with a special design (for example, in red), and redraw the fragment of the score. Since Verovio in WebView works very quickly, for the user it will look like the instant appearance of an "error note".

Here is how you can implement this, dividing it into "what" and "how":

#### What to do (concept)

1.  **Load and display the main score.** This is already working for you.
2.  **Keep a model of the score in memory (in the ViewModel).** This can be either the original XML string or, much better, a parsed Kotlin object.
3.  **When the user presses a note:**
    *   You receive a MIDI signal.
    *   You check if it matches the expected note in the score.
    *   If it does not match (error):
        *   You programmatically add information about this erroneous note to your data model (for example, in the same measure where the expected note is located).
        *   You generate new MusicXML based on this modified model. In this XML, you can mark the erroneous note in a special way, for example, add a color attribute to it.
        *   You pass this new, updated MusicXML to WebView and command Verovio to completely redraw the SVG.
4.  **Styling in Verovio:** MusicXML supports specifying the color for notes. Verovio will take this into account and draw the note in the specified color.
5.  **Returning to the initial state:** When the user presses the correct note or after a certain time, you remove the "error note" from the model and redraw the staff in its original form.

#### How to implement this (practical steps)

Let's modify your existing code.

##### 1. Improving `generateNoteXML` and `generateMusicXML`

We need to pass more information to these functions: not only a list of "correct" notes, but also an optional "error note" that the user played.
Let's change `generateNoteXML` so that it can add color:

```kotlin
/**
 * Generates XML for a single note with the ability to specify a color.
 */
private fun generateNoteXML(note: Note, duration: Int = 4, isChord: Boolean = false, color: String? = null): String {
    val (step, alter, octave) = pitchToStepOctave(note.pitch) // Modify pitchToStepOctave to return alter as well

    val colorAttribute = if (color != null) " color=\"$color\"" else ""

    // XML representation of the alteration sign (sharp)
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

Modify `pitchToStepOctave` to return a `Triple` (Step, Alter, Octave):

```kotlin
/**
 * Converts a MIDI pitch to a step, alteration, and octave.
 * pitch 60 = C4 (Middle C)
 */
private fun pitchToStepOctave(pitch: Int): Triple<String, Int, Int> {
    val steps = arrayOf("C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B")
    val alterations = arrayOf(0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0) // 0 = none, 1 = sharp

    val noteInOctave = pitch % 12
    val octave = (pitch / 12) - 1
    val step = steps[noteInOctave]
    val alter = alterations[noteInOctave]

    return Triple(step, alter, octave)
}
```

Now `generateMusicXML` should accept `playedNote`:

```kotlin
private fun generateMusicXML(originalNotes: List<Note>, playedNote: Note? = null): String {
    // ... (the beginning of the function is the same as yours)

    val allNotes = originalNotes.toMutableList()
    var playedNoteIsError = false

    if (playedNote != null) {
        // Check if the played note is among the original ones
        val isCorrect = originalNotes.any { it.pitch == playedNote.pitch }
        if (!isCorrect) {
            allNotes.add(playedNote)
            playedNoteIsError = true
        }
    }
    
    // Sort the notes by pitch for correct chord display
    allNotes.sortBy { it.pitch }

    // ... (then generate the measures)
    measuresXML.append("    <measure number=\"1\">\n")
    // ... (attributes)

    allNotes.forEachIndexed { index, note ->
        val isChord = index > 0
        var color: String? = null

        if (playedNoteIsError && note.pitch == playedNote?.pitch) {
            color = "#FF0000" // Red color for an error
        }

        measuresXML.append(generateNoteXML(note, duration = 4, isChord = isChord, color = color))
    }

    measuresXML.append("    </measure>\n")

    // ... (end of the function)

    // ... return the full XML
}
```

##### 2. PianoStaff

```kotlin
@Composable
fun PianoStaff(
    notes: List<Note>,
    playedNote: Note? = null, // Add a new state
    modifier: Modifier = Modifier
    // ...
) {
    Box(modifier = modifier) {
        // Generate MusicXML taking into account the played note
        val musicXML = generateMusicXML(notes, playedNote)
        val escapedXML = musicXML.replace(...) // As you have it

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    // ... all your settings ...
                    
                    // The HTML template will now contain update functions
                    val htmlContent = getHtmlTemplate() // Move the template to a separate function
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                // This magic will happen with every recomposition!
                // When `playedNote` changes, Compose will call this block.
                val newMusicXml = generateMusicXML(notes, playedNote)
                val newEscapedXml = newMusicXml.replace(...) // Escaping

                // Call the JS function to update the data and redraw
                webView.evaluateJavascript("updateAndRender('$newEscapedXml')", null)
            }
        )
    }
}
```

##### 3. Updating HTML and JavaScript

We need a way to update the SVG without reloading the entire page. Move your HTML template to a separate `getHtmlTemplate()` function and add the `updateAndRender` function to it:

```html
<!-- Inside the <script> tag in your HTML -->

<script>
var vrvToolkit = null;

function renderNotation(musicXml) {
    // ... (your existing code for the first render) ...
    vrvToolkit.loadData(musicXml);
    // ...
    var svg = vrvToolkit.renderToSVG(1);
    document.getElementById('notation').innerHTML = svg;
}

// NEW FUNCTION called from Kotlin
function updateAndRender(newMusicXml) {
    if (!vrvToolkit) {
        // If the Toolkit is not yet initialized, do a full render
        renderNotation(newMusicXml);
        return;
    }
    try {
        // Load new data and redraw
        vrvToolkit.loadData(newMusicXml);
        var svg = vrvToolkit.renderToSVG(1);
        document.getElementById('notation').innerHTML = svg;
    } catch (e) {
        console.error('Update error:', e);
        document.getElementById('error').innerHTML = 'Update Error: ' + e.message;
    }
}

// On initial load, call renderNotation
window.addEventListener('load', function() {
    // On first load, you can pass an empty staff
    renderNotation(getEmptyMusicXML()); // getEmptyMusicXML() should be available in JS
});
</script>
```

Instead of `renderNotation` on the first load, you can pass the initial XML from Kotlin, as you are doing now. The main thing is to have an `updateAndRender` function for subsequent updates.

**Result:**

With this approach, you are not just "highlighting" a note, but showing the user the real picture: here are the notes that should have been pressed, and here is the one (in red) that you pressed by mistake. This is much more informative for learning.

**Key moments for success:**

1.  The state (`playedNote`) is managed from outside `PianoStaff` (probably from a `ViewModel`).
2.  When the state changes, Jetpack Compose calls the `update` block of `AndroidView`.
3.  The `update` block calls the `updateAndRender` JavaScript function, passing it the completely new MusicXML.
4.  Verovio redraws the SVG, and the user sees the result.

### Two modes

Great idea! Having two modes is exactly what will make your PianoFlow application truly flexible and useful. One mode for learning ready-made pieces, the second for free creativity or analyzing your playing.

Given this dual goal, you do not need to choose just one. You need to use both approaches, but with a smart architecture so that they do not conflict, but complement each other.

*   **Verovio** will be your main engine for displaying scores.
*   **VexFlow** can be a lightweight alternative for the "free mode" if Verovio turns out to be too slow for instantly displaying single notes (which is unlikely, but possible).

However, the most elegant and maintainable approach is to use **only Verovio for both modes**, but with different data preparation logic. This will unify your drawing code and simplify maintenance.

#### A single Verovio-based architecture for two modes

Here is how you can build a system using Verovio for both tasks, right in your `PianoStaff.kt` file.

##### 1. Modifying `PianoStaff` for maximum flexibility

Your `PianoStaff` Composable should stop being "smart" (that is, stop generating XML itself). Instead, it should become a "dumb" component that simply accepts a ready-made XML string and knows how to update.

```kotlin
@Composable
fun PianoStaff(
    // Accepts an already prepared string. Who generated it is not its concern.
    musicXml: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // We use musicXml as a key. If it changes,
        // Compose will understand that it needs to recreate or update AndroidView.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                // ... creation and basic configuration of WebView ...
                WebView(context).apply {
                    // ... settings ...
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                // This function will be called on the first load AND on every update of `musicXml`.
                val escapedXML = musicXml.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")

                // It is better to move the HTML template to a separate function or string resource
                val htmlContent = getWebViewContent(escapedXML)
                webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
            }
        )
    }
}

// This function will need to be improved for updating without reloading
private fun getWebViewContent(escapedXML: String): String {
    // All your HTML code from the current implementation goes here.
    // The `musicXml` variable in JavaScript will now be `"$escapedXML"`.
    return '''
    <!DOCTYPE html>
    <html>
    <!-- ... your HTML ... -->
    <script>
        var musicXml = "$escapedXML";
        // ... your JavaScript ...
    </script>
    </html>
    '''.trimIndent()
}
```

Now your `PianoStaff` is ready for any task.

##### 2. Implementation of two modes at the ViewModel level

All the logic will now live in the `ViewModel`. The `ViewModel` will decide which MusicXML to generate and pass to `PianoStaff`.

```kotlin
// Example ViewModel
class PianoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PianoUiState())
    val uiState: StateFlow<PianoUiState> = _uiState.asStateFlow()

    // --- Mode 1: Practice with a ready-made score ---

    fun loadScoreForPractice(scoreXml: String) {
        _uiState.update {
            it.copy(
                mode = AppMode.PRACTICE,
                // Display the original score
                renderedXml = scoreXml,
                // Store the original for checking logic
                originalScoreXml = scoreXml
                // Here you can also parse the XML into Kotlin objects for convenience
            )
        }
    }

    fun onNotePlayedInPractice(playedNote: Note) {
        val currentState = _uiState.value
        val originalXml = currentState.originalScoreXml ?: return

        // 1. Check if the note is correct.
        // val isCorrect = checkNoteAgainstScore(playedNote, originalXml)
        val isCorrect = true // Simplified logic

        // 2. Generate new XML with highlighting or an "error note".
        val updatedXml = if (isCorrect) {
            // Highlight the correct note in `originalXml`
            highlightNoteInXml(originalXml, playedNote.pitch, "green")
        } else {
            // Add a red "error note" to `originalXml`
            addErrorNoteToXml(originalXml, playedNote, "red")
        }

        // 3. Update the UI
        _uiState.update { it.copy(renderedXml = updatedXml) }

        // 4. After a second, you can return to the original view
        viewModelScope.launch {
            delay(1000)
            _uiState.update { it.copy(renderedXml = originalXml) }
        }
    }


    // --- Mode 2: Free play ("Live" mode) ---

    fun enterLiveMode() {
        _uiState.update {
            it.copy(
                mode = AppMode.LIVE,
                livePlayedNotes = emptyList(),
                renderedXml = getEmptyMusicXML() // Show an empty staff
            )
        }
    }

    fun onNotePlayedInLive(playedNote: Note) {
        val currentNotes = _uiState.value.livePlayedNotes.toMutableList()
        currentNotes.add(playedNote)

        // Generate MusicXML "on the fly" from the played notes
        val liveXml = generateMusicXML(currentNotes)

        _uiState.update {
            it.copy(
                livePlayedNotes = currentNotes,
                renderedXml = liveXml
            )
        }
    }
}

// UI state model
data class PianoUiState(
    val mode: AppMode = AppMode.PRACTICE,
    val renderedXml: String = getEmptyMusicXML(),
    val originalScoreXml: String? = null,
    val livePlayedNotes: List<Note> = emptyList()
)

enum class AppMode { PRACTICE, LIVE }

// Helper functions (stubs, require implementation)
private fun highlightNoteInXml(xml: String, pitch: Int, color: String): String = xml
private fun addErrorNoteToXml(xml: String, note: Note, color: String): String = xml
```

##### 3. Putting it all together on the screen

```kotlin
@Composable
fun PianoScreen(viewModel: PianoViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column {
        // Buttons for switching modes
        Row {
            Button(onClick = { viewModel.loadScoreForPractice(somePreloadedScore) }) {
                Text("Practice Mode")
            }
            Button(onClick = { viewModel.enterLiveMode() }) {
                Text("Live Mode")
            }
        }

        // Our universal display component
        PianoStaff(
            musicXml = uiState.renderedXml,
            modifier = Modifier.weight(1f)
        )

        // Your piano component that calls onNotePlayedIn...
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

**Advantages of this approach:**

1.  **Single drawing component:** You have one `PianoStaff` that knows nothing about the modes. It just draws what it is given. This simplifies its testing and maintenance.
2.  **Centralized logic:** All the logic for generating and updating XML is in the `ViewModel`. This is the correct Separation of Concerns.
3.  **Flexibility:** You can easily add a third mode by simply adding new logic to the `ViewModel` without touching the UI components.
4.  **Preservation of your code:** You will continue to use your `generateMusicXML`, `generateNoteXML`, etc. functions for the "Live" mode. For the "Practice" mode, you will need new functions that will not generate XML from scratch, but modify the existing one (for example, using string operations or an XML parser).

**Conclusion:** Use Verovio for both modes. Make your `PianoStaff` a "dumb" component that accepts a ready-made XML string. Move all the logic for preparing this string for each of the modes to the `ViewModel`. This is the cleanest, most scalable, and most professional solution for your task.

---

## 4. Performance analysis using System Trace

Android Studio Profiler can be overloaded with information. To precisely measure the performance of key sections of code, you can use custom trace labels (`androidx.tracing.ktx.trace`).

### How to find added labels in the Profiler

1.  **Select "System Trace" mode**: In the Profiler window on the CPU track, select the **System Trace** recording configuration. This is a critical step.
2.  **Start recording**: Press the **Record** button and interact with the application for a few seconds.
3.  **Stop recording**.
4.  **Use search**: After stopping the recording, find the **search bar** (usually at the top right) and enter the name of the label you want to find (for example, `PianoStaff:WebView:update`).
5.  **Analysis**: The profiler will automatically filter all events and highlight on the timeline only the blocks you were looking for. You will be able to see their exact duration and frequency of calls.

### Useful links to official documentation

*   **Main documentation for System Trace**: [https://developer.android.com/topic/performance/tracing](https://developer.android.com/topic/performance/tracing)
    *   *Explains how to record and analyze system traces.*
*   **Documentation for custom labels (Custom Events)**: [https://developer.android.com/topic/performance/tracing/custom-events](https://developer.android.com/topic/performance/tracing/custom-events)
    *   *Describes the use of the `trace {}` block, which was added to the code.*
*   **CPU Profiler overview**: [https://developer.android.com/studio/profile/cpu-profiler](https://developer.android.com/studio/profile/cpu-profiler)
    *   *General information about the tool.*
