package com.astrizhachuk.pianoflow.domain.model

/**
 * Представляет подключенное MIDI-устройство.
 *
 * Этот класс данных содержит информацию, идентифицирующую конкретное устройство ввода или вывода MIDI,
 * такое как цифровое пианино, клавиатура или синтезатор.
 *
 * @property id Уникальный, назначенный системой идентификатор для устройства.
 * @property name Имя устройства. Для USB-устройств может состоять из имени производителя и продукта.
 * @property product Название продукта, сообщаемое самим устройством.
 * @property manufacturer Имя производителя устройства.
 */
data class MidiDevice(val id: Int, val name: String, val product: String, val manufacturer: String)
