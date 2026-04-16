package com.example.nba_salary_manager.data.model

enum class RosterTemplateCategory {
    CURRENT,
    CLASSIC
}

data class RosterPlayerTemplate(
    val name: String,
    val nbaPlayerId: Int? = null,
    val position: String,
    val role: String,
    val salaryTier: String,
    val note: String = ""
)

data class RosterTemplate(
    val id: String,
    val teamName: String,
    val seasonLabel: String,
    val category: RosterTemplateCategory,
    val summary: String,
    val playStyle: String,
    val jugadores: List<RosterPlayerTemplate>
)

object RosterTemplateRepository {
    val templates = listOf(
        RosterTemplate(
            id = "current-celtics",
            teamName = "Boston Celtics",
            seasonLabel = "Actual 2025-26",
            category = RosterTemplateCategory.CURRENT,
            summary = "Proyecto aspirante al título con aleros de élite y mucho tiro exterior.",
            playStyle = "Buen espaciado, defensa con cambios y varios generadores de juego.",
            jugadores = listOf(
                RosterPlayerTemplate("Jayson Tatum", 1628369, "SF", "Franquicia", "Max", "Anotador principal"),
                RosterPlayerTemplate("Jaylen Brown", 1627759, "SG", "Coestrella", "Max", "Presión hacia el aro"),
                RosterPlayerTemplate("Derrick White", 1628401, "PG", "Titular", "Núcleo", "Conexión en ambos lados"),
                RosterPlayerTemplate("Sam Hauser", 1630573, "SF", "Rotación", "Valor", "Especialista en tiro tras recepción"),
                RosterPlayerTemplate("Nikola Vucevic", 202696, "C", "Titular", "Veterano", "Punto de apoyo interior"),
                RosterPlayerTemplate("Payton Pritchard", 1630202, "PG", "Sexto hombre", "Valor", "Ritmo y tiro desde el banquillo")
            )
        ),
        RosterTemplate(
            id = "current-thunder",
            teamName = "Oklahoma City Thunder",
            seasonLabel = "Actual 2025-26",
            category = RosterTemplateCategory.CURRENT,
            summary = "Núcleo joven con tamaño, defensa y mucha versatilidad.",
            playStyle = "Ritmo alto, ayudas agresivas y cinco jugadores muy conectados.",
            jugadores = listOf(
                RosterPlayerTemplate("Shai Gilgeous-Alexander", 1628983, "PG", "Franquicia", "Supermax", "Motor ofensivo"),
                RosterPlayerTemplate("Jalen Williams", 1631114, "SF", "Coestrella", "Max", "Generador secundario con balón"),
                RosterPlayerTemplate("Chet Holmgren", 1631096, "C", "Titular", "Núcleo", "Protección de aro y espacios"),
                RosterPlayerTemplate("Alex Caruso", 1627936, "SG", "Titular", "Veterano", "Defensa de impacto"),
                RosterPlayerTemplate("Isaiah Hartenstein", 1628392, "C", "Rotación", "Núcleo", "Rebote y bloqueos"),
                RosterPlayerTemplate("Luguentz Dort", 1629652, "SG", "Titular", "Nucleo", "Especialista defensivo exterior")
            )
        ),
        RosterTemplate(
            id = "current-nuggets",
            teamName = "Denver Nuggets",
            seasonLabel = "Actual 2025-26",
            category = RosterTemplateCategory.CURRENT,
            summary = "Plantilla construida alrededor de un creador total con continuidad estructural.",
            playStyle = "Juego desde el codo, bloqueos mano a mano y lectura colectiva.",
            jugadores = listOf(
                RosterPlayerTemplate("Nikola Jokic", 203999, "C", "Franquicia", "Supermax", "Centro creador del ataque"),
                RosterPlayerTemplate("Jamal Murray", 1627750, "PG", "Coestrella", "Max", "Bloqueo directo principal"),
                RosterPlayerTemplate("Aaron Gordon", 203932, "PF", "Titular", "Núcleo", "Finalización y defensa"),
                RosterPlayerTemplate("Cameron Johnson", 1629661, "SF", "Titular", "Titular", "Tiro y tamaño"),
                RosterPlayerTemplate("Bruce Brown", 1628971, "SG", "Rotación", "Veterano", "Pegamento táctico"),
                RosterPlayerTemplate("Christian Braun", 1631128, "SG", "Titular", "Titular", "Energía y transición")
            )
        ),
        RosterTemplate(
            id = "classic-bulls",
            teamName = "Chicago Bulls",
            seasonLabel = "Clásica 1995-96",
            category = RosterTemplateCategory.CLASSIC,
            summary = "Ejemplo icónico de jerarquía clara, defensa y ejecución en media pista.",
            playStyle = "Triángulo ofensivo, presión defensiva y control del ritmo.",
            jugadores = listOf(
                RosterPlayerTemplate("Michael Jordan", 893, "SG", "Franquicia", "Leyenda", "Cierre de partidos"),
                RosterPlayerTemplate("Scottie Pippen", 937, "SF", "Coestrella", "Élite", "Creador secundario"),
                RosterPlayerTemplate("Dennis Rodman", 23, "PF", "Titular", "Especialista", "Rebote y energía"),
                RosterPlayerTemplate("Ron Harper", 252, "PG", "Titular", "Titular", "Tamaño en el perímetro"),
                RosterPlayerTemplate("Luc Longley", 778, "C", "Titular", "Titular", "Bloqueos y presencia interior"),
                RosterPlayerTemplate("Toni Kukoc", 389, "SF", "Sexto hombre", "Lujo", "Generador desde el banquillo")
            )
        ),
        RosterTemplate(
            id = "classic-lakers",
            teamName = "Los Angeles Lakers",
            seasonLabel = "Clásica 2000-01",
            category = RosterTemplateCategory.CLASSIC,
            summary = "Modelo de duo dominante rodeado de roles muy definidos.",
            playStyle = "Juego interior, gravedad de estrellas y complementos físicos.",
            jugadores = listOf(
                RosterPlayerTemplate("Shaquille O'Neal", 406, "C", "Franquicia", "Leyenda", "Ventaja interior"),
                RosterPlayerTemplate("Kobe Bryant", 977, "SG", "Coestrella", "Élite", "Anotación de alto volumen"),
                RosterPlayerTemplate("Derek Fisher", 220, "PG", "Titular", "Titular", "Control y tiro abierto"),
                RosterPlayerTemplate("Rick Fox", 279, "SF", "Titular", "Titular", "Alero de equilibrio"),
                RosterPlayerTemplate("Horace Grant", 189, "PF", "Titular", "Veterano", "Trabajo sucio"),
                RosterPlayerTemplate("Robert Horry", 306, "PF", "Rotación", "Decisivo", "Espacios y defensa situacional")
            )
        ),
        RosterTemplate(
            id = "classic-warriors",
            teamName = "Golden State Warriors",
            seasonLabel = "Clásica 2016-17",
            category = RosterTemplateCategory.CLASSIC,
            summary = "Plantilla de movimiento continuo con cinco amenazas reales.",
            playStyle = "Movimiento sin balon, lectura rapida y quintetos bajos muy efectivos.",
            jugadores = listOf(
                RosterPlayerTemplate("Stephen Curry", 201939, "PG", "Franquicia", "Max", "Gravedad total"),
                RosterPlayerTemplate("Klay Thompson", 202691, "SG", "Coestrella", "Max", "Tiro de volumen"),
                RosterPlayerTemplate("Kevin Durant", 201142, "SF", "Franquicia", "Max", "Ventaja constante en los emparejamientos"),
                RosterPlayerTemplate("Draymond Green", 203110, "PF", "Titular", "Núcleo", "Ancla defensiva"),
                RosterPlayerTemplate("Andre Iguodala", 2738, "SF", "Sexto hombre", "Veterano", "Cierre de partidos"),
                RosterPlayerTemplate("Zaza Pachulia", 2585, "C", "Titular", "Titular", "Bloqueos y presencia física")
            )
        )
    )
}
