package app.discord.patches.banner

import app.morphe.patcher.patch.resourcePatch

/**
 * Hides the quest promo banner (QuestBar) by neutering its visibility gate
 * directly in the Hermes bundle.
 *
 * Target analysis (Hermes bytecode v98 in both builds):
 *
 * - 342.16 Stable: gate is function 59152 (frame 251, 387 bytes).
 * - 343.12 Stable: gate is function 59938 (frame 49, 387 bytes).
 *
 * Each gate reads getDeliveredQuest(), bails on null quest / userStatus,
 * honors isDismissed, dispatches AdCreativeType QUEST vs BOUNTY, and
 * renders QuestContent.QUEST_BAR_MOBILE. None contains try/catch.
 *
 * The edit replaces the gate's first two instructions (6 bytes:
 * GetParentEnvironment + LoadParam) with:
 *   LoadConstNull r2 (94 02) + Ret r2 (76 02) + LoadConstUndefined r0 (93 00)
 * The gate returns null on entry; decoder alignment is preserved and the
 * rest of the body is unreachable but intact. Verified per version: the
 * edited bundle re-disassembles with this as the ONLY difference across
 * all ~125k functions.
 *
 * 342 and 343 share byte-identical gate codegen, so one anchor covers
 * both stable builds. The patch applies it exactly once and fails loudly
 * otherwise, so a Discord codegen change can never silently corrupt the
 * bundle.
 */
val hideQuestBarPatch = resourcePatch(
    name = "Hide quest promo banner",
    description = "Hides the quest promo banner at the top of the server channel list.",
    default = true,
) {
    compatibleWith(DiscordConstants.COMPATIBILITY_DISCORD)

    execute {
        val replacement = b("94 02 76 02 93 00")
        // 342.16 / 343.12 gate (fn 59152 / 59938, identical codegen).
        val anchor = b("34 03 00 89 0a 01 3b 0b 03 00 3b 09 03 02 5e 04")

        val bundle = get("assets/index.android.bundle", true)
        val bytes = bundle.readBytes().toMutableList()

        val hits = findAll(bytes, anchor)
        check(hits.size == 1) {
            "QuestBar gate anchor matched ${hits.size} time(s); " +
                "Discord likely changed the bundle - patch needs re-analysis."
        }

        val at = hits[0]
        replacement.forEachIndexed { i, byte -> bytes[at + i] = byte }
        bundle.writeBytes(bytes.toByteArray())
    }
}

private fun b(hex: String): ByteArray =
    hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()

private fun findAll(haystack: List<Byte>, needle: ByteArray): List<Int> {
    val out = mutableListOf<Int>()
    if (needle.isEmpty() || haystack.size < needle.size) return out
    outer@ for (i in 0..haystack.size - needle.size) {
        for (j in needle.indices) {
            if (haystack[i + j] != needle[j]) continue@outer
        }
        out.add(i)
    }
    return out
}
