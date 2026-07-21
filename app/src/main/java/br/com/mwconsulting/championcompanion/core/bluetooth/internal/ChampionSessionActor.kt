package br.com.mwconsulting.championcompanion.core.bluetooth.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal class ChampionSessionActor<Message>(
    scope: CoroutineScope,
    private val onMessage: suspend (Message) -> Unit,
) {
    private val mailbox = Channel<Message>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (message in mailbox) {
                onMessage(message)
            }
        }
    }

    suspend fun send(message: Message) {
        mailbox.send(message)
    }

    fun close() {
        mailbox.close()
    }
}
