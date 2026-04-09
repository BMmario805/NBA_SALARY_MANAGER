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
    val players: List<RosterPlayerTemplate>
)

object RosterTemplateRepository {
    val templates = listOf(
        RosterTemplate(
            id = "current-celtics",
            teamName = "Boston Celtics",
            seasonLabel = "Actual 2025-26",
            category = RosterTemplateCategory.CURRENT,
            summary = "Base de contender con aleros de elite y mucho tiro exterior.",
            playStyle = "Espaciado, defensa de cambios y varios creadores.",
            players = listOf(
                RosterPlayerTemplate("Jayson Tatum", 1628369, "SF", "Franquicia", "Max", "Anotador principal"),
                RosterPlayerTemplate("Jaylen Brown", 1627759, "SG", "Co-estrella", "Max", "Presion al aro"),
                RosterPlayerTemplate("Derrick White", 1628401, "PG", "Titular", "Core", "Conexion en ambos lados"),
                RosterPlayerTemplate("Sam Hauser", 1630573, "SF", "Rotacion", "Value", "Especialista en catch-and-shoot"),
                RosterPlayerTemplate("Nikola Vucevic", 202696, "C", "Titular", "Veterano", "Punto de apoyo interior"),
                RosterPlayerTemplate("Payton Pritchard", 1630202, "PG", "Sexto hombre", "Value", "Ritmo y tiro desde banca")
            )
        ),
        RosterTemplate(
            id = "current-thunder",
            teamName = "Oklahoma City Thunder",
            seasonLabel = "Actual 2025-26",
            category = RosterTemplateCategory.CURRENT,
            summary = "Nucleo joven con longitud, defensa y mucha versatilidad.",
            playStyle = "Ritmo alto, ayudas agresivas y cinco jugadores conectados.",
            players = listOf(
                RosterPlayerTemplate("Shai Gilgeous-Alexander", 1628983, "PG", "Franquicia", "Supermax", "Motor ofensivo"),
                RosterPlayerTemplate("Jalen Williams", 1631114, "SF", "Co-estrella", "Max", "Secundario con bola"),
                RosterPlayerTemplate("Chet Holmgren", 1631096, "C", "Titular", "Core", "Proteccion de aro y spacing"),
                RosterPlayerTemplate("Alex Caruso", 1627936, "SG", "Titular", "Veterano", "Defensa de impacto"),
                RosterPlayerTemplate("Isaiah Hartenstein", 1628392, "C", "Rotacion", "Core", "Rebote y bloqueos"),
                RosterPlayerTemplate("Luguentz Dort", 1629652, "SG", "Titular", "Core", "Stopper exterior")
            )
        ),
        RosterTemplate(
            id = "current-nuggets",
            teamName = "Denver Nuggets",
            seasonLabel = "Actual 2025-26",
            category = RosterTemplateCategory.CURRENT,
            summary = "Plantilla alrededor de un creador total con continuidad estructural.",
            playStyle = "Juego desde codo, bloqueos mano a mano y lectura colectiva.",
            players = listOf(
                RosterPlayerTemplate("Nikola Jokic", 203999, "C", "Franquicia", "Supermax", "Hub ofensivo"),
                RosterPlayerTemplate("Jamal Murray", 1627750, "PG", "Co-estrella", "Max", "Pick and roll primario"),
                RosterPlayerTemplate("Aaron Gordon", 203932, "PF", "Titular", "Core", "Finalizador y defensa"),
                RosterPlayerTemplate("Cameron Johnson", 1629661, "SF", "Titular", "Starter", "Tiro y tamano"),
                RosterPlayerTemplate("Bruce Brown", 1628971, "SG", "Rotacion", "Veterano", "Pegamento tactico"),
                RosterPlayerTemplate("Christian Braun", 1631128, "SG", "Titular", "Starter", "Energia y transicion")
            )
        ),
        RosterTemplate(
            id = "classic-bulls",
            teamName = "Chicago Bulls",
            seasonLabel = "Clasica 1995-96",
            category = RosterTemplateCategory.CLASSIC,
            summary = "Ejemplo iconico de jerarquia clara, defensa y ejecucion en media pista.",
            playStyle = "Triangulo ofensivo, presion defensiva y control del ritmo.",
            players = listOf(
                RosterPlayerTemplate("Michael Jordan", 893, "SG", "Franquicia", "GOAT tier", "Closer absoluto"),
                RosterPlayerTemplate("Scottie Pippen", 937, "SF", "Co-estrella", "Elite", "Creador secundario"),
                RosterPlayerTemplate("Dennis Rodman", 23, "PF", "Titular", "Especialista", "Rebote y energia"),
                RosterPlayerTemplate("Ron Harper", 252, "PG", "Titular", "Starter", "Tamano en el perimetro"),
                RosterPlayerTemplate("Luc Longley", 778, "C", "Titular", "Starter", "Bloqueos y presencia interior"),
                RosterPlayerTemplate("Toni Kukoc", 389, "SF", "Sexto hombre", "Luxury", "Generador desde banca")
            )
        ),
        RosterTemplate(
            id = "classic-lakers",
            teamName = "Los Angeles Lakers",
            seasonLabel = "Clasica 2000-01",
            category = RosterTemplateCategory.CLASSIC,
            summary = "Modelo de duo dominante rodeado de roles muy definidos.",
            playStyle = "Juego interior, gravedad de estrellas y complementos fisicos.",
            players = listOf(
                RosterPlayerTemplate("Shaquille O'Neal", 406, "C", "Franquicia", "GOAT tier", "Ventaja interior"),
                RosterPlayerTemplate("Kobe Bryant", 977, "SG", "Co-estrella", "Elite", "Anotacion de alto volumen"),
                RosterPlayerTemplate("Derek Fisher", 220, "PG", "Titular", "Starter", "Control y tiro abierto"),
                RosterPlayerTemplate("Rick Fox", 279, "SF", "Titular", "Starter", "Alero de equilibrio"),
                RosterPlayerTemplate("Horace Grant", 189, "PF", "Titular", "Veterano", "Trabajo sucio"),
                RosterPlayerTemplate("Robert Horry", 306, "PF", "Rotacion", "Clutch", "Spacing y defensa situacional")
            )
        ),
        RosterTemplate(
            id = "classic-warriors",
            teamName = "Golden State Warriors",
            seasonLabel = "Clasica 2016-17",
            category = RosterTemplateCategory.CLASSIC,
            summary = "Plantilla de movimiento continuo con cinco amenazas reales.",
            playStyle = "Movimiento sin balon, lectura rapida y small-ball letal.",
            players = listOf(
                RosterPlayerTemplate("Stephen Curry", 201939, "PG", "Franquicia", "Max", "Gravedad total"),
                RosterPlayerTemplate("Klay Thompson", 202691, "SG", "Co-estrella", "Max", "Tiro de volumen"),
                RosterPlayerTemplate("Kevin Durant", 201142, "SF", "Franquicia", "Max", "Mismatch permanente"),
                RosterPlayerTemplate("Draymond Green", 203110, "PF", "Titular", "Core", "Ancla defensiva"),
                RosterPlayerTemplate("Andre Iguodala", 2738, "SF", "Sexto hombre", "Veterano", "Cierre de partidos"),
                RosterPlayerTemplate("Zaza Pachulia", 2585, "C", "Titular", "Starter", "Bloqueos y cuerpo")
            )
        )
    )
}
