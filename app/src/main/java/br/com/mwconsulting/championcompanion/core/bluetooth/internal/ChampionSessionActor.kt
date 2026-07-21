package br.com.mwconsulting.championcompanion.core.bluetooth.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
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
        try {
            mailbox.send(message)
        } catch (_: ClosedSendChannelException) {
            // Cleanup paths can race on disconnect/close; a closed mailbox means the session is already torn down.
        }
    }

    fun close() {
        mailbox.close()
    }
}
