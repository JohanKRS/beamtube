package ytslim.ui

/**
 * Quando os cartões de uma lista podem deslizar para o lugar novo (pedido do Johan, 2026-11-03).
 * Animam quando vídeos saem da lista (tirar do Histórico, bloquear um canal) ou chegam no fim (a
 * próxima página). Numa lista nova (puxar para atualizar, ou as Inscrições guardadas trocadas pelas
 * novas), os cartões que continuam iriam "voar" até a posição nova: aí eles só aparecem no lugar.
 */
object ListMotion {
    fun animates(before: List<Any>, after: List<Any>): Boolean {
        val position = HashMap<Any, Int>(before.size * 2)
        before.forEachIndexed { index, key -> position[key] = index }
        var last = -1
        var added = false
        for (key in after) {
            val index = position[key]
            if (index == null) {
                added = true
                continue
            }
            // Um que já estava, depois de um novo ou fora da ordem de antes: a lista mudou toda.
            if (added || index < last) return false
            last = index
        }
        return true
    }
}
