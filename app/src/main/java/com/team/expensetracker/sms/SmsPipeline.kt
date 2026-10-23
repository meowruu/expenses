package com.team.expensetracker.sms

/**
 * Global pipeline holder for SMS ingestion.
 *
 * During early development, [ingestor] defaults to [StubSmsHandler].
 *
 * AT INTEGRATION:
 * B1's parser (which parses financial details and rejects OTPs) and B2's repository
 * (which persists deduplicated transactions in Room) will replace [StubSmsHandler]
 * with a production [SmsIngestor] implementation (e.g. LiveSmsHandler).
 *
 * Both [SmsReceiver] and [SimulatedSmsSource] route through [ingestor].
 */
object SmsPipeline {

    /**
     * Active ingestor instance processing intercepted SMS messages.
     * Marked @Volatile to ensure thread-safe visibility across background receiver threads.
     */
    @Volatile
    var ingestor: SmsIngestor = StubSmsHandler()
}
