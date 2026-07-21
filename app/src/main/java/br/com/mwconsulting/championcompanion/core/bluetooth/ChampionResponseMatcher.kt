package br.com.mwconsulting.championcompanion.core.bluetooth

object ChampionResponseMatchers {
    fun bySession(
        sessionId: ChampionSessionId,
        delegate: ChampionResponseMatcher? = null,
    ): ChampionResponseMatcher = ChampionResponseMatcher { packet ->
        packet.sessionId == sessionId && (delegate?.matches(packet) ?: true)
    }

    fun byChannel(
        sessionId: ChampionSessionId,
        channel: ChampionChannelAlias,
        delegate: ChampionResponseMatcher? = null,
    ): ChampionResponseMatcher = ChampionResponseMatcher { packet ->
        packet.sessionId == sessionId &&
            packet.channel == channel &&
            (delegate?.matches(packet) ?: true)
    }

    fun byChannelAndCommand(
        sessionId: ChampionSessionId,
        channel: ChampionChannelAlias,
        commandCode: Int,
        delegate: ChampionResponseMatcher? = null,
    ): ChampionResponseMatcher = ChampionResponseMatcher { packet ->
        packet.sessionId == sessionId &&
            packet.channel == channel &&
            packet.commandCode == commandCode &&
            (delegate?.matches(packet) ?: true)
    }

    fun forCommand(
        sessionId: ChampionSessionId,
        command: ChampionCommand,
    ): ChampionResponseMatcher = command.responseMatcher?.let { matcher ->
        byChannel(sessionId = sessionId, channel = command.channel, delegate = matcher)
    } ?: byChannelAndCommand(
        sessionId = sessionId,
        channel = command.channel,
        commandCode = command.commandCode,
    )
}
