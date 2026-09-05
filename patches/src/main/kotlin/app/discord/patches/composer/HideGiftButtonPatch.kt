package app.discord.patches.composer

import app.discord.patches.banner.DiscordConstants
import app.morphe.patcher.patch.resourcePatch

/**
 * Hides the gift (Nitro gifting) button in the chat message composer by
 * skipping its actions.push() directly in the Hermes bundle.
 *
 * Target analysis (Hermes bytecode v98 in all three builds):
 *
 * - 343.12 Stable: composer is function 51985, gift push is
 *   Call2 r13, r15, r12, r13 at file offset 30962861.
 * - 342.16 Stable: composer is function 51714, gift push at file
 *   offset 30851579.
 * - 341.13 Stable: composer is function 51354, gift push at file
 *   offset 30703459.
 *
 * Each composer builds an `actions` array with conditional pushes. The
 * gift slot is `if (shouldShowGiftButton) actions.push({NITRO_GIFT...})`
 * with the THREAD action as the else-branch (gift XOR thread), so the
 * flag itself is NOT forced: that would show the thread button instead.
 * The edit replaces the 5-byte gift push
 * (Call2 r13, r15, r12, r13 = 6E 0D 0F 0C 0D) with:
 *   Mov r13, r13 (10 0D 0D) + LoadConstFalse r13 (96 0D)
 * Both scratch only r13, which the THREAD branch reassigns
 * (NewObjectWithBuffer) before any read, as does r15 (GetByIdShort);
 * the Jmp over the THREAD block and the length-based width math below
 * are untouched and adapt to the shorter array at runtime.
 *
 * 342 and 341 share byte-identical gift-push codegen, so two anchors
 * cover all three builds. The patch tries each anchor and applies the
 * one found exactly once; anything else fails loudly so a Discord
 * codegen change can never silently corrupt the bundle.
 */
val hideGiftButtonPatch = resourcePatch(
    name = "Hide gift button",
    description = "Hides the gift button in the chat message composer.",
    default = true,
) {
    compatibleWith(DiscordConstants.COMPATIBILITY_DISCORD_COMPOSER)

    execute {
        val replacement = b("10 0D 0D 96 0D")
        val anchors = listOf(
            // 342.16 / 341.13 gift push (identical codegen).
            b("6E 0D 0F 0C 0D AE 20 44 0F 0C 21 C8 02 0D AA 00"),
            // 343.12 gift push.
            b("6E 0D 0F 0C 0D AE 1C 44 0F 0C 24 C8 01 0D AA 00"),
        )

        val bundle = get("assets/index.android.bundle", true)
        val bytes = bundle.readBytes().toMutableList()

        val matched = anchors.map { it to findAll(bytes, it) }
            .filter { (_, hits) -> hits.isNotEmpty() }
        check(matched.size == 1 && matched[0].second.size == 1) {
            "Gift push anchor matched ${matched.sumOf { it.second.size }} " +
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
