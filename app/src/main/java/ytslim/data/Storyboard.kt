package ytslim.data

import com.liskovsoft.mediaserviceinterfaces.data.MediaItemStoryboard

/**
 * As miniaturas da linha do tempo (o "storyboard" do YouTube), para o preview ao arrastar a barra:
 * folhas JPEG com [cols] x [rows] quadros, um a cada [intervalMs] do vídeo. O motor lê a
 * especificação junto com os formatos do vídeo; o app só escolhe o quadro e baixa a folha.
 */
class Storyboard(
    val cols: Int,
    val rows: Int,
    val intervalMs: Long,
    private val sheetUrl: (Int) -> String,
) {
    /** Um quadro: a folha e a coluna e a linha dentro dela. */
    data class Frame(val sheet: Int, val col: Int, val row: Int)

    private val perSheet = cols * rows

    fun frameAt(positionMs: Long): Frame {
        val index = (positionMs.coerceAtLeast(0) / intervalMs).toInt()
        val inSheet = index % perSheet
        return Frame(index / perSheet, inSheet % cols, inSheet / cols)
    }

    /** Quantas folhas um vídeo de [durationMs] tem (0 = não se sabe a duração). */
    fun sheetCount(durationMs: Long): Int =
        if (durationMs <= 0) 0 else ((durationMs - 1) / (intervalMs * perSheet) + 1).toInt()

    fun url(sheet: Int): String = sheetUrl(sheet)

    companion object {
        fun from(source: MediaItemStoryboard?): Storyboard? = runCatching {
            // getGroupSize antes de getGroupUrl: o motor guarda o tamanho escolhido nessa chamada.
            val size = source?.groupSize ?: return null
            if (size.colCount <= 0 || size.rowCount <= 0 || size.durationEachMS <= 0) return null
            Storyboard(size.colCount, size.rowCount, size.durationEachMS.toLong()) { source.getGroupUrl(it) }
        }.getOrNull()
    }
}
