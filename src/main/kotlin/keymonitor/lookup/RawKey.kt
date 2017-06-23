package keymonitor.lookup

import keymonitor.common.encodeBytesAsHex


/** In libsignal, raw keys are represented as byte arrays */
typealias RawKey = ByteArray

/** In our app, we will encode keys as strings */
typealias KeyValue = String

/**
 * We'll also need to deal with several keys at a time.
 * For convenience, we'll treat these as a single key value.
 */
typealias KeyList = String

/**
 * Convert a raw key to its in-app representation, which is a hex-encoded string with no spaces
 */
fun RawKey.toKeyValue(): KeyValue {
    return encodeBytesAsHex(this)
}

/** Return a KeyList (string representation) of a collection of raw keys */
fun toKeyList(rawKeys: Collection<RawKey>): KeyList {
    return rawKeys.map { it.toKeyValue() }.joinToString(",")
}
