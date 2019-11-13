/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.interactor

import android.content.Context
import android.net.Uri
import com.moez.QKSMS.compat.TelephonyCompat
import com.moez.QKSMS.model.Attachment
import com.moez.QKSMS.repository.ScheduledMessageRepository
import io.realm.RealmList
import javax.inject.Inject

class SendScheduledMessage @Inject constructor(
    private val context: Context,
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val sendMessage: SendMessage
) : Interactor<Long>() {

    override suspend fun execute(params: Long) {
        val scheduledMessage = scheduledMessageRepo.getScheduledMessage(params) ?: return
        val messages = when {
            scheduledMessage.sendAsGroup -> listOf(scheduledMessage)
            else -> scheduledMessage.recipients.map { recipient ->
                scheduledMessage.copy(recipients = RealmList(recipient))
            }
        }

        messages.forEach { message ->
            val threadId = TelephonyCompat.getOrCreateThreadId(context, message.recipients)
            val attachments = message.attachments.mapNotNull(Uri::parse).map { uri -> Attachment.Image(uri) }
            sendMessage.execute(SendMessage.Params(message.subId, threadId, message.recipients, message.body,
                    attachments))

            scheduledMessageRepo.deleteScheduledMessage(message.id)
        }
    }

}