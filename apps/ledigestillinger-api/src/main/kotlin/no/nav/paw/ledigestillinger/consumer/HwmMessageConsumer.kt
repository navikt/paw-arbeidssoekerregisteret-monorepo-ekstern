package no.nav.paw.ledigestillinger.consumer

import no.nav.paw.hwm.DataConsumer
import no.nav.paw.hwm.Message

typealias HwmMessageConsumer<K, V> = DataConsumer<Message<K, V>, K, V>