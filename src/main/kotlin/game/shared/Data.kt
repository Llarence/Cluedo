package game.shared

import java.awt.Color
import java.io.Serializable

enum class People(private val readableName: String, val color: Color) {
    PLUM("Plum", Color(149, 46, 255)),
    WHITE("White", Color(162, 162, 162)),
    SCARLET("Scarlet", Color(203, 0, 83)),
    GREEN("Green", Color(65, 255, 61)),
    MUSTARD("Mustard", Color(218, 139, 0)),
    PEACOCK("Peacock", Color(0, 98, 211));

    override fun toString(): String {
        return readableName
    }
}

enum class Weapons(private val readableName: String) {
    ROPE("Rope"),
    DAGGER("Dagger"),
    WRENCH("Wrench"),
    PISTOL("Pistol"),
    CANDLESTICK("Candlestick"),
    LEAD_PIPE("Lead Pipe");

    override fun toString(): String {
        return readableName
    }
}

enum class Rooms(private val readableName: String) {
    KITCHEN("Kitchen"),
    BALL_ROOM("Ball Room"),
    CONSERVATORY("Conservatory"),
    BILLIARD_ROOM("Billiard Room"),
    LIBRARY("Library"),
    STUDY("Study"),
    HALL("Hall"),
    LOUNGE("Lounge"),
    DINING_ROOM("Dining Room");

    override fun toString(): String {
        return readableName
    }
}

abstract class Card : Serializable

class PersonCard(val person: People) : Card() {
    override fun toString(): String {
        return person.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is PersonCard) {
            return person == other.person
        }

        return false
    }

    override fun hashCode(): Int {
        return person.hashCode()
    }
}

class WeaponCard(val weapon: Weapons) : Card() {
    override fun toString(): String {
        return weapon.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is WeaponCard) {
            return weapon == other.weapon
        }

        return false
    }

    override fun hashCode(): Int {
        return weapon.hashCode()
    }
}

class RoomCard(val room: Rooms) : Card() {
    override fun toString(): String {
        return room.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is RoomCard) {
            return room == other.room
        }

        return false
    }

    override fun hashCode(): Int {
        return room.hashCode()
    }
}

abstract class Location : Serializable

class PointLocation(val x: Int, val y: Int) : Location() {
    override fun equals(other: Any?): Boolean {
        if (other is PointLocation) {
            return x == other.x && y == other.y
        }

        return false
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }
}

// Null is Cluedo room
class RoomLocation(val room: Rooms?) : Location() {
    override fun equals(other: Any?): Boolean {
        if (other is RoomLocation) {
            return room == other.room
        }

        return false
    }

    override fun hashCode(): Int {
        return room?.hashCode() ?: 0
    }
}

class PlayerData(val person: People, val location: Location) : Serializable

fun genCards(): Array<Card> {
    val cards = mutableListOf<Card>()

    for (person in People.values()) {
        cards.add(PersonCard(person))
    }

    for (weapon in Weapons.values()) {
        cards.add(WeaponCard(weapon))
    }

    for (room in Rooms.values()) {
        cards.add(RoomCard(room))
    }

    return cards.toTypedArray()
}