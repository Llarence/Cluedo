package game.shared

import java.io.Serializable

abstract class Packet

class IsObserver(val isObserver: Boolean) : Packet(), Serializable

class RequestPlayerPick : Packet(), Serializable

class RespondPlayerPick(val person: People) : Packet(), Serializable

class AlertPlayerPick(val person: People) : Packet(), Serializable

class AlertHand(val cards: Array<Card>) : Packet(), Serializable

class AlertPublicCards(val cards: Array<Card>) : Packet(), Serializable

class RequestMoveTo(val moves: Int) : Packet(), Serializable

class RespondMoveTo(val location: Location) : Packet(), Serializable

class AlertMoveTo(val playerData: PlayerData) : Packet(), Serializable

class RequestRumor : Packet(), Serializable

class RespondRumor(val person: People, val weapon: Weapons) : Packet(), Serializable

class AlertRumor(val person: People, val weapon: Weapons) : Packet(), Serializable

class RequestCounterExample : Packet(), Serializable

class RespondCounterExample(val card: Card?) : Packet(), Serializable

class AlertCounterExample(val person: People, val card: Card) : Packet(), Serializable

class RequestClueGuess : Packet(), Serializable

class RespondClueGuess(val person: People, val weapon: Weapons, val room: Rooms) : Packet(), Serializable

class AlertClueGuess(val person: People, val weapon: Weapons, val room: Rooms) : Packet(), Serializable

class AlertWin(val winner: People) : Packet(), Serializable

class AlertElimination(val eliminatee: People) : Packet(), Serializable

class ChatMessage(val person: People, val text: String) : Serializable