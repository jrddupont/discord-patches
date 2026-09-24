package app.discord.patches.banner

import app.morphe.patcher.patch.resourcePatch

/**
 * Hides the quest promo banner (QuestBar) by neutering its visibility gate
 * directly in the Hermes bundle.
 *
 * Target analysis (Hermes bytecode v98 in all four builds):
 *
 * - 346.13 Stable: gate is function 62203 (offset 33116380, 535 bytes,
 *   frame 198). Same checks as before (getDeliveredQuest, null/userStatus,
 *   isDismissed, QuestContent.QUEST_BAR_MOBILE) plus new eligibility
 *   hooks (useIsQuestExpired, getIsEligibleForQuests,
 *   getDeliveredAdCreativeId). Head gained a CreateFunctionEnvironment
 *   (GPE + CFE + LoadParam), so the 6-byte edit overwrites GPE + CFE
 *   instead — gate still returns null on entry. Own 17-byte anchor
 *   (5 whole instructions).
 * - 344.13 Stable: gate is function 60429 (offset 32664515, 477 bytes,
 *   frame 79). Same shape as the 342/343 gate (getDeliveredQuest,
 *   null/userStatus checks, isDismissed, QuestContent.QUEST_BAR_MOBILE)
 *   with shifted registers, so it gets its own anchor.
 * - 342.16 Stable: gate is function 59152 (frame 251, 387 bytes).
 * - 343.12 Stable: gate is function 59938 (frame 49, 387 bytes).
 * - 341.13 Stable: gate is function 58782 (50 regs, 279 bytes).
 *
 * The 342/343 gates read getDeliveredQuest(), bail on null quest /
 * userStatus, honor isDismissed, dispatch AdCreativeType QUEST vs BOUNTY,
 * and render QuestContent.QUEST_BAR_MOBILE. The 341 gate is hook-style
 * (quest arrives via parent, no getDeliveredQuest call inside) but enforces
 * the same null quest / userStatus / isDismissed checks before rendering
 * QUEST_BAR_MOBILE. None contains try/catch.
 *
 * The edit replaces the gate's first two instructions (6 bytes:
 * GetParentEnvironment + LoadParam, or GetParentEnvironment +
 * CreateFunctionEnvironment on 346.13) with:
 *   LoadConstNull r2 (94 02) + Ret r2 (76 02) + LoadConstUndefined r0 (93 00)
 * The gate returns null on entry; decoder alignment is preserved and the
 * rest of the body is unreachable but intact. Verified per version: the
 * edited bundle re-disassembles with this as the ONLY difference across
 * all ~125k functions.
 *
 * 342 and 343 share byte-identical gate codegen; 341, 344, and 346 differ,
 * so four anchors cover all five builds. The patch tries each anchor and applies
 * the one found exactly once; anything else fails loudly so a Discord
 * codegen change can never silently corrupt the bundle.
 */
val hideQuestBarPatch = resourcePatch(
    name = "Hide quest promo banner",
    description = "Hides the quest promo banner at the top of the server channel list.",
    default = true,
) {
    compatibleWith(DiscordConstants.COMPATIBILITY_DISCORD)

    execute {
        val replacement = b("94 02 76 02 93 00")
        val anchors = listOf(
            // 342.16 / 343.12 gate (fn 59152 / 59938, identical codegen).
            b("34 03 00 89 0a 01 3b 0b 03 00 3b 09 03 02 5e 04"),
            // 341.13 gate (fn 58782, offset 32203161).
            b("34 03 00 89 04 01 3b 06 03 01 3b 08 03 02 5e 05"),
            // 344.13 gate (fn 60429, offset 32664515).
            b("34 03 00 89 0b 01 3b 0c 03 00 3b 0a 03 02 5e 04"),
            // 346.13 gate (fn 62203, offset 33116380; head is GPE + CFE).
            b("34 0e 00 40 05 02 89 0c 01 37 05 00 0c 3b 0d 0e 00"),
        )

        val bundle = get("assets/index.android.bundle", true)
        val bytes = bundle.readBytes().toMutableList()

        val matched = anchors.map { it to findAll(bytes, it) }
            .filter { (_, hits) -> hits.isNotEmpty() }
        check(matched.size == 1 && matched[0].second.size == 1) {
            "QuestBar gate anchor matched ${matched.sumOf { it.second.size }} " +
                "time(s) across ${matched.size} known pattern(s); " +
                "Discord likely changed the bundle - patch needs re-analysis."
        }

        val at = matched[0].second[0]
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
