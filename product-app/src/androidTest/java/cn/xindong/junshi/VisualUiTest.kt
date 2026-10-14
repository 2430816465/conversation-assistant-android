package cn.xindong.junshi

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import cn.xindong.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Render synthetic fixtures on the explicitly selected emulator. Never a real AI result. */
class VisualUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val c get()=(compose.activity.application as JunshiApp).controller
    private fun ready() { compose.waitForIdle(); compose.waitUntil(20_000) { !c.busy }; compose.waitForIdle() }
    private fun click(text: String,substring: Boolean=false) {
        val target=compose.onNodeWithText(text,substring=substring)
        if(!target.isDisplayed()) target.performScrollTo()
        target.performClick(); compose.waitForIdle()
    }
    @Test fun captureResponsiveScreensAndReachImportantControls() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val label=InstrumentationRegistry.getArguments().getString("visualLabel") ?: "default-phone-light"
        require(label.matches(Regex("[a-z0-9-]+")))
        val output=File(compose.activity.filesDir,"qa-ui/$label").apply { mkdirs() }
        val observations=mutableListOf<JSONObject>()
        var completed=false
        val apkDigest=MessageDigest.getInstance("SHA-256")
        File(compose.activity.applicationInfo.sourceDir).inputStream().use { input ->
            val block=ByteArray(1024*1024)
            while(true) { val count=input.read(block); if(count<0) break; apkDigest.update(block,0,count) }
        }
        val apkSha=apkDigest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        File(output,"observations.json").writeText(JSONObject().put("completed",false).put("apkSha256",apkSha).toString())
        fun capture(name: String) {
            compose.waitForIdle()
            // Compose can be idle while Android is still animating a Dialog window.
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(450)
            compose.waitForIdle()
            val bitmap=instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
            File(output,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            observations+=JSONObject().put("screen",name).put("width",bitmap.width).put("height",bitmap.height).put("capturedAt",System.currentTimeMillis())
                .put("synthetic",true).put("aiRequest",false)
            bitmap.recycle()
        }
        fun nav(tag: String) {
            compose.onNodeWithTag("nav-$tag").assertIsDisplayed().performClick(); compose.waitForIdle()
            listOf("ADVISOR","HISTORY","PROFILE","ME").forEach {
                val bounds=compose.onNodeWithTag("nav-$it").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertTrue("Navigation touch target below 48dp",bounds.height>=48*compose.activity.resources.displayMetrics.density)
            }
        }
        ready(); compose.runOnUiThread { c.lock() }; ready(); capture("01-login")
        compose.onNodeWithTag("account-submit").performScrollTo().assertIsDisplayed()
        compose.runOnUiThread { c.createAccount("演示账户 "+newId().take(4),"synthetic-password") }; ready()
        val account=c.account!!
        try {
            compose.runOnUiThread { c.createStreamer("演示主播 · 合成资料") }; ready()
            compose.runOnUiThread { c.createContact("小满 · 合成资料") }; ready()
            compose.runOnUiThread { c.importText("\t观众\tTEXT\t合成：今天工作有点累，想早点休息。\n\t主播\tTEXT\t好呀，早点休息，明天再聊。") }; ready()
            nav("ADVISOR"); capture("02-advisor")
            compose.onNode(hasText("对方最新消息 1（原文）") and hasSetTextAction()).performScrollTo()
            capture("03-latest-review")
            compose.onNodeWithText("确认最新消息并分析").assertIsNotEnabled()
            compose.onNode(hasText("对方最新消息 1（原文）") and hasSetTextAction()).performTextReplacement("合成：今天工作有点累。")
            compose.waitForIdle()
            click("属于当前观众 小满 · 合成资料")
            compose.onNodeWithText("确认最新消息并分析").assertIsNotEnabled()
            nav("HISTORY"); capture("04-history-import")
            click(c.current!!.batches.single().id.take(6)+" · ",true)
            compose.onNodeWithText("已逐项核对本批确属当前观众",substring=true).performScrollTo()
            capture("05-history-review")
            nav("PROFILE"); capture("06-profile")
            compose.onNode(hasText("历史完整度与缺口") and hasSetTextAction()).performScrollTo().assertIsDisplayed()
            nav("ME"); capture("07-my")
            click("模型与组件"); capture("08-models")
            click("添加接口配置"); capture("09-model-editor")
            compose.onNode(hasText("API Key") and hasSetTextAction()).performScrollTo().assertIsDisplayed()
            capture("10-model-key")
            compose.onNodeWithContentDescription("取消接口编辑").performClick()
            compose.onNodeWithContentDescription("返回我的").performClick()
            click("费用与预算"); capture("11-budget")
            compose.onNodeWithContentDescription("返回我的").performClick()
            click("使用指南"); capture("12-guide")
            // Own-app synthetic reply layout only. No provider result, score, charge or send.
            val conversation=c.current!!
            val analysis=AnalysisFormat.parse("""{"analysis":{"facts":[],"hypotheses":[],"unknowns":["合成UI夹具，非AI结果"],"emotion":"合成","relationship":"合成","disagreements":[],"strategy":"合成布局验证","next_steps":[],"history_limits":"无真实历史"},"no_reply":false,"no_reply_reason":"","candidates":[{"id":"synthetic-c1","style":"温柔简短 · 合成示例","segments":["合成：那今天就早点休息呀","合成：明天再慢慢聊"],"rationale":"合成内容，仅用于布局检查","risks":[],"scores":{"natural":0,"style_fit":0,"segmentation":0,"length_fit":0,"context_fit":0},"evidence_ids":[]}],"new_topics":[],"suggested_tags":[]}""",emptySet())
            val round=RoundResult(scope=conversation.scope,startedAt=System.currentTimeMillis(),model="SYNTHETIC_UI_ONLY",coverage=Coverage(0,0,"synthetic",emptyList(),emptyList(),0,"非AI质量证据"),components=emptyList(),memories=emptyList(),analysis=analysis)
            compose.runOnUiThread { c.vault.saveRound(round); c.selectContact(conversation) }; ready()
            nav("ADVISOR"); click("已保存的分析"); click("分析记录：请选择"); click("SYNTHETIC_UI_ONLY",true)
            compose.onNodeWithText("选择并编辑").performScrollTo().assertIsDisplayed(); capture("13-reply-options")
            click("选择并编辑"); capture("14-reply-editor")
            click("保存草稿并收起")
            assertTrue(c.charges.isEmpty()); assertFalse(c.generating)
            completed=true
        } finally {
            compose.runOnUiThread { c.deleteAccount() }; ready()
            File(output,"observations.json").writeText(JSONObject().put("label",label).put("fontScale",compose.activity.resources.configuration.fontScale)
                .put("densityDpi",compose.activity.resources.configuration.densityDpi).put("screens",arr(observations))
                .put("completed",completed).put("versionName",BuildConfig.VERSION_NAME).put("versionCode",BuildConfig.VERSION_CODE).put("apkSha256",apkSha)
                .put("note","Synthetic own-app rendering only; no real WeChat or AI acceptance.").toString(2))
        }
    }
}

