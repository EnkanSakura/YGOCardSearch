package top.enkansakura

import kotlinx.serialization.ExperimentalSerializationApi
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.IllegalStateException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class YgocdbRequester(private val subject: Contact) {

    private lateinit var response: okhttp3.Response
    private val headers = Headers.headersOf(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.99 Safari/537.36 Edg/97.0.1072.69"
        )
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().let {
        it.readTimeout(5, TimeUnit.SECONDS)
        it.writeTimeout(5, TimeUnit.SECONDS)
        it.connectTimeout(5, TimeUnit.SECONDS)
        it.build()
    }

    private fun getImage(imageUrl: String): ByteArrayOutputStream {
        val infoStream = ByteArrayOutputStream()
        try {
            val imageResponse = okHttpClient.newCall(Request.Builder()
                .headers(headers)
                .let {
                    it.url(imageUrl)
                    it.build()
                }).execute()
            val `in` = imageResponse.body?.byteStream()
            val buffer = ByteArray(2048)
            var len = 0
            if (`in` != null) {
                while (`in`.read(buffer).also { len = it } > 0) {
                    infoStream.write(buffer, 0, len)
                }
            }
            infoStream.write((Math.random() * 100).toInt() + 1)
            infoStream.close()
            return infoStream
        } catch (e:Exception) {
            e.printStackTrace()
            return infoStream
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun request(url: String) {
        try {
            response = okHttpClient.newCall(Request.Builder()
                .headers(headers)
                .let {
                    it.url(url)
                    it.build()
            }).execute()
            if (response.code == 200) {
                val htmlStr = response.body!!.string()
                val document: Document = Jsoup.parse(htmlStr)
                val searchResult: Element? = document.select("div.card").first()
                if (searchResult != null) {
                    YGOCardSearch.logger.debug(searchResult.text())
                    val cardInfo: Card = Card(
                        img = searchResult.select("img")[0].dataset()["original"].toString(),
//                        zh = searchResult.select("h2")[0].text().trim(),
//                        jp = searchResult.select("h3")[0].text().trim(),
//                        en = searchResult.select("h3")[1].text().trim(),
//                        id = searchResult.select("h3")[2].text().trim(),
                        desc = searchResult.select("div.desc").html()
                            .replace(
                                Regex("</*?div.*?>|</*?strong.*?>|</*?a.*?>|</*?span.*?>|</*?br>",
                                    RegexOption.IGNORE_CASE),
                                ""
                            ).replace(
                                Regex("</*?hr>", RegexOption.IGNORE_CASE), "\n"
                            )
                    )

                    val toExternalResource = getImage(cardInfo.img).toByteArray().toExternalResource()
                    val imageId: String = toExternalResource.uploadAsImage(subject).imageId
                    toExternalResource.close()

                    subject.sendMessage(buildMessageChain {
                        +Image(imageId)
//                        +PlainText(cardInfo.img)
                        +PlainText("\n" + cardInfo.desc)
                    })
                }
                else {
                    subject.sendMessage("没有找到这张卡捏")
                }
            } else {
                subject.sendMessage("请求出错，，")
            }
        } catch (e: IllegalStateException) {
            subject.sendMessage("图片发送失败，，")
        } catch (e: SocketTimeoutException){
            subject.sendMessage("请求超时，，")
        } catch (e: SocketException) {
            subject.sendMessage("连接出错，，")
        } catch (e: Throwable) {
            subject.sendMessage("查卡出错了，，")
            YGOCardSearch.logger.error(e)
        } finally {
            response.close()
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            okHttpClient.cache?.close()
        }
    }

}