package top.enkansakura

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info

object YGOCardSearch : KotlinPlugin(
    JvmPluginDescription(
        id = "top.enkansakura.plugin.ygo-card",
        version = "1.0-SNAPSHOT",
        name = "YgoCardSearch"
    ) {
        author("EnkanSakura")
        info("YGO卡查")
    }
) {
    override fun onEnable() {
        logger.info { "YgoCardSearch loaded" }
        YgoCommand.register()

    }

    override fun onDisable() {
        YgoCommand.unregister()
    }
}


object YgoCommand : SimpleCommand(
    YGOCardSearch, "ygo", "查卡",
    description = "YGO卡查"
) {
    @Handler
    suspend fun CommandSender.handle(cardName: String) {
        val sender = this.subject?.id
        if (sender == null) {
            this.sendMessage(cardName)
            return
        }
        else {
            YGOCardSearch.logger.info("正在获取 $cardName 的信息")
            YgocdbRequester(subject!!).request(
                "https://ygocdb.com/?search=$cardName"
            )
        }
    }
}
