package ytslim.ui

/** 754000 -> "12:34"; 3_723_000 -> "1:02:03". */
fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return buildString(8) {
        if (h > 0) {
            append(h).append(':')
            if (m < 10) append('0')
        }
        append(m).append(':')
        if (s < 10) append('0')
        append(s)
    }
}
