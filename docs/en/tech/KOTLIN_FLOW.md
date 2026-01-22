## 5. Deep Dive: Kotlin Flow

`Flow` is a key component of the Kotlin coroutines library for working with asynchronous data streams.

### What is a Flow?

A `Flow` is a type that can sequentially emit multiple values. It can be thought of as a pipe through which data is asynchronously transmitted: from a producer that creates it to a consumer that collects it.

A key feature of a basic `Flow` is that it is **"cold"**. This means that the code inside the `Flow` is not executed until someone subscribes to it (that is, calls a terminal operator on it, such as `collect`). Each new subscriber starts a new, independent data stream.

### "Hot" Streams: `StateFlow` and `SharedFlow`

Unlike "cold" `Flow`s, **"hot"** streams exist independently of the presence of subscribers. They can store state and send updates to everyone who is subscribed to them. This makes them ideal for use in Android applications.

*   **`StateFlow`**: This is a "hot" stream that always has a value (initial or last sent). It is ideal for storing the screen state (UI State) in a `ViewModel`. It always has only one, the most current element, and it immediately gives it to each new subscriber. If the new value is identical to the old one, the update is not sent.

*   **`SharedFlow`**: This is a more flexible "hot" stream designed for events that must be delivered to **all** subscribers. Unlike `StateFlow`, it has no initial value by default. It is ideal for sending one-time events, such as navigation commands or showing a `Snackbar`, as in our case with `UserNotifier`. It can be fine-tuned:
    *   `replay`: How many recent events to "remember" and resend to new subscribers. We used `replay = 1` in our `FakeUserNotifier` to make the test more reliable.
    *   `extraBufferCapacity`: How many events to store in the buffer if subscribers do not have time to process them.

### Resources for study

*   **Official Kotlin documentation**: (The most complete and accurate information)
    *   [Asynchronous Flows (Flow)](https://kotlinlang.org/docs/flow.html) - The main reference.
    *   [Flow context and dispatchers](https://kotlinlang.org/docs/flow.html#flow-context) - (Section in the main article).
    *   [Error handling in Flow](https://kotlinlang.org/docs/flow.html#flow-exceptions) - (Section in the main article).

*   **Guides for Android developers**: (Practical application in Android)
    *   [Overview of Kotlin Flow in Android](https://developer.android.com/kotlin/flow) - A great starting point.
    *   [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) - A detailed explanation of the difference and use cases.
    *   [Testing Kotlin Flow](https://developer.android.com/kotlin/flow/test) - The official guide to the techniques we used.
    *   [Additional resources for coroutines and Flow](https://developer.android.com/kotlin/coroutines/additional-resources) - A collection of articles and videos from the Android team.