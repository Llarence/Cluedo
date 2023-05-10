package game.shared

// Lots of ways to optimize, but I am too lazy
object Board {
    fun personToDataAtStart(person: People): PlayerData {
        return when(person) {
            People.PLUM -> PlayerData(person, PointLocation(23, 5))
            People.WHITE -> PlayerData(person, PointLocation(9, 24))
            People.SCARLET -> PlayerData(person, PointLocation(7, 0))
            People.GREEN -> PlayerData(person, PointLocation(14, 24))
            People.MUSTARD -> PlayerData(person, PointLocation(0, 7))
            People.PEACOCK -> PlayerData(person, PointLocation(23, 18))
        }
    }

    fun getPossibleMoves(locationFrom: Location, moves: Int, players: Array<PlayerData>): Array<Location> {
        val possibleMoves = mutableListOf<Location>()

        var currTiles = mutableListOf<PointLocation>()
        if (locationFrom is RoomLocation) {
            val room = locationFrom.room

            if (room != null) {
                possibleMoves.add(RoomLocation(roomConnectedToRoom(room)))

                // Super lazy
                for (x in 0..24) {
                    for (y in 0..25) {
                        if (tileConnectedToRoom(x, y) == room) {
                            currTiles.add(PointLocation(x, y))
                        }
                    }
                }
            } else {
                return possibleMoves.toTypedArray()
            }
        } else {
            currTiles.add(locationFrom as PointLocation)
        }

        var newTiles = mutableListOf<PointLocation>()
        for (i in 0..if (locationFrom is RoomLocation) { moves - 1} else { moves }) {
            for (tile in currTiles) {
                if (isTile(tile.x, tile.y) && !possibleMoves.any { if (it is PointLocation) { it.x == tile.x && it.y == tile.y } else { false } }) {
                    // Could make function
                    var occupied = false
                    for (testPlayer in players) {
                        if (testPlayer.location is PointLocation) {
                            val testLocation: PointLocation = testPlayer.location

                            if (tile.x == testLocation.x && tile.y == testLocation.x) {
                                occupied = true
                            }
                        }
                    }

                    if (!occupied) {
                        possibleMoves.add(tile)
                    }

                    newTiles.add(PointLocation(tile.x - 1, tile.y))
                    newTiles.add(PointLocation(tile.x + 1, tile.y))
                    newTiles.add(PointLocation(tile.x, tile.y - 1))
                    newTiles.add(PointLocation(tile.x, tile.y + 1))

                    val connected = tileConnectedToRoom(tile.x, tile.y)
                    if (tileConnectedToRoom(tile.x, tile.y) != null && !possibleMoves.any { if (it is RoomLocation) { it.room == connected } else { false } }) {
                        if (locationFrom is RoomLocation) {
                            if (locationFrom.room == connected) {
                                continue
                            }
                        }

                        possibleMoves.add(RoomLocation(connected))
                    } else {
                        if (tileConnectedToCluedo(tile.x, tile.y) && !possibleMoves.any { if (it is RoomLocation) { it.room == null } else { false } }) {
                            possibleMoves.add(RoomLocation(null))
                        }
                    }
                }
            }

            currTiles = newTiles
            newTiles = mutableListOf()
        }

        possibleMoves.remove(locationFrom)
        return possibleMoves.toTypedArray()
    }

    // Could be own faster function not just wrapper like
    fun isLegal(locationFrom: Location, locationTo: Location, moves: Int, players: Array<PlayerData>): Boolean {
        val possibleMoves = getPossibleMoves(locationFrom, moves, players)

        return possibleMoves.any { it == locationTo }
    }

    private fun tileConnectedToCluedo(x: Int, y: Int): Boolean {
        return x == 12 && y == 7
    }

    private fun tileConnectedToRoom(x: Int, y: Int): Rooms? {
        if (x == 4 && y == 17) {
            return Rooms.KITCHEN
        }

        if (x == 6 && y == 6) {
            return Rooms.LOUNGE
        }

        if ((x == 6 && y == 8) || (x == 8 && y == 12)) {
            return Rooms.DINING_ROOM
        }

        if ((x == 7 && y == 19) || (x == 9 && y == 16) || (x == 14 && y == 16) || (x == 16 && y == 19)) {
            return Rooms.BALL_ROOM
        }

        if ((x == 11 && y == 7) || (x == 12 && y == 7)) {
            return Rooms.HALL
        }

        if (x == 18 && y == 19) {
            return Rooms.CONSERVATORY
        }

        if ((x == 17 && y == 15) || (x == 22 && y == 11)) {
            return Rooms.BILLIARD_ROOM
        }

        if ((x == 20 && y == 11) || (x == 16 && y == 7)) {
            return Rooms.LIBRARY
        }

        if (x == 17 && y == 4) {
            return Rooms.STUDY
        }

        return null
    }

    private fun roomConnectedToRoom(room: Rooms): Rooms? {
        return when (room) {
            Rooms.KITCHEN -> Rooms.STUDY
            Rooms.LOUNGE -> Rooms.CONSERVATORY
            Rooms.STUDY -> Rooms.KITCHEN
            Rooms.CONSERVATORY -> Rooms.LOUNGE
            else -> null
        }
    }

    fun isInClue(x: Int, y: Int): Boolean {
        return x in 10..14 && y in 8..14
    }

    fun getRoom(x: Int, y: Int): Rooms? {
        if (!(x == 0 && y == 18) && x in 0..5 && y in 18..23) {
            return Rooms.KITCHEN
        }

        if ((x in 8..15 && y in 17..22) || (x in 10..13 && y in 23..24)) {
            return Rooms.BALL_ROOM
        }

        if (!(x == 18 && y == 19) && !(x == 23 && y == 19) && x in 18..23 && y in 19..23) {
            return Rooms.CONSERVATORY
        }

        if ((x in 0..4 && y in 9..15) || (x in 5..7 && y in 9..14)) {
            return Rooms.DINING_ROOM
        }

        if (x in 18..23 && y in 12..16) {
            return Rooms.BILLIARD_ROOM
        }

        if (((x == 17 || x == 23) && y in 7..9) || (x in 18..22 && y in 6..10)) {
            return Rooms.LIBRARY
        }

        if (!(x == 17 && y == 0) && x in 17..23 && y in 0..3) {
            return Rooms.STUDY
        }

        if (x in 9..14 && y in 0..6) {
            return Rooms.HALL
        }

        if (!(x == 6 && y == 0) && x in 0..6 && y in 0..5) {
            return Rooms.LOUNGE
        }

        return null
    }

   fun isTile(x: Int, y: Int): Boolean {
        return (x == 0 && y == 7) ||
            (x == 0 && y == 17) ||
            (x == 1 && y == 6) ||
            (x == 1 && y == 7) ||
            (x == 1 && y == 8) ||
            (x == 1 && y == 16) ||
            (x == 1 && y == 17) ||
            (x == 2 && y == 6) ||
            (x == 2 && y == 7) ||
            (x == 2 && y == 8) ||
            (x == 2 && y == 16) ||
            (x == 2 && y == 17) ||
            (x == 3 && y == 6) ||
            (x == 3 && y == 7) ||
            (x == 3 && y == 8) ||
            (x == 3 && y == 16) ||
            (x == 3 && y == 17) ||
            (x == 4 && y == 6) ||
            (x == 4 && y == 7) ||
            (x == 4 && y == 8) ||
            (x == 4 && y == 16) ||
            (x == 4 && y == 17) ||
            (x == 5 && y == 6) ||
            (x == 5 && y == 7) ||
            (x == 5 && y == 8) ||
            (x == 5 && y == 15) ||
            (x == 5 && y == 16) ||
            (x == 5 && y == 17) ||
            (x == 6 && y == 6) ||
            (x == 6 && y == 7) ||
            (x == 6 && y == 8) ||
            (x == 6 && y == 15) ||
            (x == 6 && y == 16) ||
            (x == 6 && y == 17) ||
            (x == 6 && y == 18) ||
            (x == 6 && y == 19) ||
            (x == 6 && y == 20) ||
            (x == 6 && y == 21) ||
            (x == 6 && y == 22) ||
            (x == 7 && y == 0) ||
            (x == 7 && y == 1) ||
            (x == 7 && y == 2) ||
            (x == 7 && y == 3) ||
            (x == 7 && y == 4) ||
            (x == 7 && y == 5) ||
            (x == 7 && y == 6) ||
            (x == 7 && y == 7) ||
            (x == 7 && y == 8) ||
            (x == 7 && y == 15) ||
            (x == 7 && y == 16) ||
            (x == 7 && y == 17) ||
            (x == 7 && y == 18) ||
            (x == 7 && y == 19) ||
            (x == 7 && y == 20) ||
            (x == 7 && y == 21) ||
            (x == 7 && y == 22) ||
            (x == 7 && y == 23) ||
            (x == 8 && y == 1) ||
            (x == 8 && y == 2) ||
            (x == 8 && y == 3) ||
            (x == 8 && y == 4) ||
            (x == 8 && y == 5) ||
            (x == 8 && y == 6) ||
            (x == 8 && y == 7) ||
            (x == 8 && y == 8) ||
            (x == 8 && y == 9) ||
            (x == 8 && y == 10) ||
            (x == 8 && y == 11) ||
            (x == 8 && y == 12) ||
            (x == 8 && y == 13) ||
            (x == 8 && y == 14) ||
            (x == 8 && y == 15) ||
            (x == 8 && y == 16) ||
            (x == 8 && y == 23) ||
            (x == 9 && y == 7) ||
            (x == 9 && y == 8) ||
            (x == 9 && y == 9) ||
            (x == 9 && y == 10) ||
            (x == 9 && y == 11) ||
            (x == 9 && y == 12) ||
            (x == 9 && y == 13) ||
            (x == 9 && y == 14) ||
            (x == 9 && y == 15) ||
            (x == 9 && y == 16) ||
            (x == 9 && y == 23) ||
            (x == 9 && y == 24) ||
            (x == 10 && y == 7) ||
            (x == 10 && y == 15) ||
            (x == 10 && y == 16) ||
            (x == 11 && y == 7) ||
            (x == 11 && y == 15) ||
            (x == 11 && y == 16) ||
            (x == 12 && y == 7) ||
            (x == 12 && y == 15) ||
            (x == 12 && y == 16) ||
            (x == 13 && y == 7) ||
            (x == 13 && y == 15) ||
            (x == 13 && y == 16) ||
            (x == 14 && y == 7) ||
            (x == 14 && y == 15) ||
            (x == 14 && y == 16) ||
            (x == 14 && y == 23) ||
            (x == 14 && y == 24) ||
            (x == 15 && y == 1) ||
            (x == 15 && y == 2) ||
            (x == 15 && y == 3) ||
            (x == 15 && y == 4) ||
            (x == 15 && y == 5) ||
            (x == 15 && y == 6) ||
            (x == 15 && y == 7) ||
            (x == 15 && y == 8) ||
            (x == 15 && y == 9) ||
            (x == 15 && y == 10) ||
            (x == 15 && y == 11) ||
            (x == 15 && y == 12) ||
            (x == 15 && y == 13) ||
            (x == 15 && y == 14) ||
            (x == 15 && y == 15) ||
            (x == 15 && y == 16) ||
            (x == 15 && y == 23) ||
            (x == 16 && y == 0) ||
            (x == 16 && y == 1) ||
            (x == 16 && y == 2) ||
            (x == 16 && y == 3) ||
            (x == 16 && y == 4) ||
            (x == 16 && y == 5) ||
            (x == 16 && y == 6) ||
            (x == 16 && y == 7) ||
            (x == 16 && y == 8) ||
            (x == 16 && y == 9) ||
            (x == 16 && y == 10) ||
            (x == 16 && y == 11) ||
            (x == 16 && y == 12) ||
            (x == 16 && y == 13) ||
            (x == 16 && y == 14) ||
            (x == 16 && y == 15) ||
            (x == 16 && y == 16) ||
            (x == 16 && y == 17) ||
            (x == 16 && y == 18) ||
            (x == 16 && y == 19) ||
            (x == 16 && y == 20) ||
            (x == 16 && y == 21) ||
            (x == 16 && y == 22) ||
            (x == 16 && y == 23) ||
            (x == 17 && y == 4) ||
            (x == 17 && y == 5) ||
            (x == 17 && y == 6) ||
            (x == 17 && y == 10) ||
            (x == 17 && y == 11) ||
            (x == 17 && y == 12) ||
            (x == 17 && y == 13) ||
            (x == 17 && y == 14) ||
            (x == 17 && y == 15) ||
            (x == 17 && y == 16) ||
            (x == 17 && y == 17) ||
            (x == 17 && y == 18) ||
            (x == 17 && y == 19) ||
            (x == 17 && y == 20) ||
            (x == 17 && y == 21) ||
            (x == 17 && y == 22) ||
            (x == 18 && y == 4) ||
            (x == 18 && y == 5) ||
            (x == 18 && y == 11) ||
            (x == 18 && y == 17) ||
            (x == 18 && y == 18) ||
            (x == 18 && y == 19) ||
            (x == 19 && y == 4) ||
            (x == 19 && y == 5) ||
            (x == 19 && y == 11) ||
            (x == 19 && y == 17) ||
            (x == 19 && y == 18) ||
            (x == 20 && y == 4) ||
            (x == 20 && y == 5) ||
            (x == 20 && y == 11) ||
            (x == 20 && y == 17) ||
            (x == 20 && y == 18) ||
            (x == 21 && y == 4) ||
            (x == 21 && y == 5) ||
            (x == 21 && y == 11) ||
            (x == 21 && y == 17) ||
            (x == 21 && y == 18) ||
            (x == 22 && y == 4) ||
            (x == 22 && y == 5) ||
            (x == 22 && y == 11) ||
            (x == 22 && y == 17) ||
            (x == 22 && y == 18) ||
            (x == 23 && y == 5) ||
            (x == 23 && y == 18)
    }
}