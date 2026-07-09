package com.example.puntodeventa.ui.home

data class EmojiEntry(
    val emoji: String,
    val tags: List<String>
)

val defaultEmojiList: List<EmojiEntry> = listOf(
    // Food & Drink
    EmojiEntry("🌮", listOf("taco", "food", "mexico", "comida")),
    EmojiEntry("🍕", listOf("pizza", "food", "italiano", "comida")),
    EmojiEntry("🍔", listOf("hamburguesa", "burger", "food", "comida")),
    EmojiEntry("🍜", listOf("ramen", "noodles", "sopa", "food", "comida")),
    EmojiEntry("🍣", listOf("sushi", "japonés", "food", "comida")),
    EmojiEntry("🥗", listOf("ensalada", "salad", "saludable", "food", "comida")),
    EmojiEntry("🍩", listOf("dona", "donut", "postre", "dulce", "comida")),
    EmojiEntry("☕", listOf("café", "coffee", "bebida", "caliente")),
    EmojiEntry("🧃", listOf("jugo", "juice", "bebida", "fruta")),
    EmojiEntry("🍺", listOf("cerveza", "beer", "bebida", "alcohol")),
    EmojiEntry("🌯", listOf("burrito", "wrap", "food", "comida", "mexico")),
    EmojiEntry("🍗", listOf("pollo", "chicken", "food", "comida")),
    EmojiEntry("🥩", listOf("carne", "steak", "meat", "food", "comida")),
    EmojiEntry("🍰", listOf("pastel", "cake", "postre", "dulce")),
    EmojiEntry("🥤", listOf("refresco", "soda", "bebida", "frio")),
    EmojiEntry("🍦", listOf("helado", "ice cream", "postre", "dulce")),
    // Faces
    EmojiEntry("😀", listOf("feliz", "happy", "sonrisa", "cara")),
    EmojiEntry("😊", listOf("sonrisa", "smile", "feliz", "cara")),
    EmojiEntry("🥰", listOf("amor", "love", "corazon", "cara")),
    EmojiEntry("😎", listOf("genial", "cool", "gafas", "cara")),
    EmojiEntry("🤩", listOf("estrella", "star", "emocionado", "cara")),
    EmojiEntry("😋", listOf("rico", "yummy", "sabroso", "cara")),
    EmojiEntry("🤔", listOf("pensando", "thinking", "duda", "cara")),
    EmojiEntry("😏", listOf("pícaro", "smirk", "cara")),
    // Animals
    EmojiEntry("🐱", listOf("gato", "cat", "animal")),
    EmojiEntry("🐶", listOf("perro", "dog", "animal")),
    EmojiEntry("🦊", listOf("zorro", "fox", "animal")),
    EmojiEntry("🐻", listOf("oso", "bear", "animal")),
    EmojiEntry("🐼", listOf("panda", "animal")),
    EmojiEntry("🐸", listOf("rana", "frog", "animal")),
    // Misc
    EmojiEntry("🌍", listOf("mundo", "earth", "planeta", "global")),
    EmojiEntry("⭐", listOf("estrella", "star", "favorito")),
    EmojiEntry("🎉", listOf("fiesta", "party", "celebración")),
    EmojiEntry("🏆", listOf("trofeo", "trophy", "ganador", "premio")),
    EmojiEntry("🔥", listOf("fuego", "fire", "caliente", "picante")),
    EmojiEntry("💎", listOf("diamante", "diamond", "lujo", "premium")),
)
