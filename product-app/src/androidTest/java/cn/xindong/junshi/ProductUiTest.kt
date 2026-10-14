package cn.xindong.junshi

import android.content.ClipboardManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.xindong.core.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic own-app workflows only. No provider requests and no WeChat acceptance. */
class ProductUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val c get()=(compose.activity.application as JunshiApp).controller
    private val pin="synthetic-password"
    private fun ready() { compose.waitForIdle(); compose.waitUntil(20_000) { !c.busy }; compose.waitForIdle() }
    private fun node(text: String,substring: Boolean=false)=compose.onNodeWithText(text,substring=substring)
    private fun visible(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        val target=compose.onNode(matcher)
        if(!target.isDisplayed()) {
            if(compose.onAllNodesWithTag("model-editor-scroll").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("model-editor-scroll").performScrollToNode(matcher)
            } else target.performScrollTo()
        }
        return target.assertIsDisplayed()
    }
    private fun click(text: String,substring: Boolean=false) { visible(hasText(text,substring=substring)).performClick(); compose.waitForIdle() }
    private fun field(label: String)=compose.onNode(hasText(label) and hasSetTextAction())
    private fun fill(label: String,value: String) { visible(hasText(label) and hasSetTextAction()).performTextReplacement(value); compose.waitForIdle() }
    private fun nav(tag: String) { compose.onNodeWithTag("nav-$tag").performClick(); compose.waitForIdle() }
    private fun start(withContact: Boolean=true): Account {
        ready(); compose.runOnUiThread { c.lock(); c.createAccount("SYNTHETIC-"+newId().take(8),pin) }; ready()
        val account=c.account!!
        if(withContact) {
            compose.runOnUiThread { c.createStreamer("合成主播") }; ready()
            compose.runOnUiThread { c.createContact("合成观众") }; ready()
        }
        return account
    }
    private fun clean(account: Account,password: String=pin) {
        ready()
        if(c.account?.id!=account.id) { compose.runOnUiThread { c.unlock(account,password) }; ready() }
        compose.runOnUiThread { c.deleteAccount() }; ready()
        assertNull(c.current); assertNull(c.vault.unlocked); assertFalse(ReadBridge.autoRead)
    }

    @Test fun registrationValidationPasswordChangeAndLoginRetainErrors() {
        ready(); compose.runOnUiThread { c.lock() }; ready()
        if(compose.onAllNodesWithTag("account-confirmation").fetchSemanticsNodes().isEmpty()) click("注册本机账号")
        compose.onNodeWithTag("account-name").performTextInput("SYNTHETIC-"+newId().take(8))
        compose.onNodeWithTag("account-password").performTextInput(pin)
        compose.onNodeWithTag("account-confirmation").performTextInput("different-password")
        compose.onNodeWithTag("account-submit").performScrollTo().performClick()
        node("两次输入的密码不一致").assertExists(); assertNull(c.account)
        compose.onNodeWithTag("account-confirmation").performTextReplacement(pin)
        compose.onNodeWithTag("account-submit").performScrollTo().performClick(); ready()
        val account=c.account!!; var next=pin
        try {
            nav("ME"); click("修改密码")
            fill("当前密码",pin); fill("新密码（至少6位）","synthetic-new-password"); fill("确认新密码","synthetic-new-password")
            click("保存新密码"); ready(); next="synthetic-new-password"
            node("账户密码已修改",true).assertExists()
            compose.onNodeWithContentDescription("退出并锁定账户").performClick(); click("退出登录"); ready()
            compose.onNodeWithTag("account-name").performTextReplacement(account.name)
            compose.onNodeWithTag("account-password").performTextReplacement(pin)
            compose.onNodeWithTag("account-submit").performScrollTo().performClick(); ready()
            node("本地账户口令不正确",true).assertExists()
            compose.onNodeWithTag("account-name").assertTextContains(account.name); assertNull(c.account)
            android.os.SystemClock.sleep(1_100)
            compose.onNodeWithTag("account-password").performTextReplacement(next)
            compose.onNodeWithTag("account-submit").performScrollTo().performClick(); ready()
            assertEquals(account.id,c.account!!.id)
        } finally { clean(account,next) }
    }

    @Test fun historyReviewDraftSurvivesNavigationAndOnlyConfirmedRowsEnterHistory() {
        val account=start()
        try {
            nav("HISTORY")
            fill("微信复制文字或逐行消息","\t观众\tTEXT\t合成：今天有点累\n\t主播\tTEXT\t那早点休息呀")
            click("导入待校对区"); ready(); assertEquals(0,c.current!!.messages.size)
            val batch=c.current!!.batches.single()
            click(batch.id.take(6)+" · ",true)
            click("已逐项核对本批确属当前观众",true)
            nav("PROFILE"); nav("HISTORY")
            node("已逐项核对本批确属当前观众",true).assertIsOn()
            compose.activityRule.scenario.recreate(); ready()
            node("已逐项核对本批确属当前观众",true).assertIsOn()
            click("已核对本批从旧到新的顺序",true)
            val review="已核对正文、说话人及时间；主播行确认为实际已发送"
            compose.onAllNodesWithText(review)[0].performScrollTo().performClick()
            node("确认入库").assertIsNotEnabled()
            compose.onAllNodesWithText(review)[1].performScrollTo().performClick()
            click("确认入库"); ready()
            assertEquals(c.status,2,c.current!!.messages.size); assertTrue(c.current!!.messages.last().actualSent)
            fill("查找历史原文","今天有点累")
            node("合成：今天有点累").performScrollTo().assertIsDisplayed()
            assertEquals(2,c.current!!.messages.size)
            compose.onNodeWithContentDescription("退出并锁定账户").performClick(); click("退出登录"); ready()
            assertNull(c.current); assertNull(c.vault.unlocked); compose.onNodeWithTag("account-submit").assertExists()
        } finally { clean(account) }
    }

    @Test fun reviewedLatestMessageShowsRealMissingConfigErrorAndRejectsDuplicate() {
        val account=start()
        try {
            fill("对方最新消息 1（原文）","合成：明天八点见")
            nav("PROFILE"); nav("ADVISOR")
            field("对方最新消息 1（原文）").assertTextContains("合成：明天八点见")
            compose.activityRule.scenario.recreate(); ready()
            field("对方最新消息 1（原文）").assertTextContains("合成：明天八点见")
            node("确认最新消息并分析").assertIsNotEnabled()
            click("属于当前观众 合成观众")
            click("均为对方原文，正文与说话人已核对")
            click("按顺序排列的最新未回复消息，尚未入库")
            click("确认最新消息并分析"); ready()
            compose.waitUntil(20_000) { !c.generating }; compose.waitForIdle()
            assertEquals(1,c.current!!.messages.size)
            assertEquals("合成：明天八点见",c.current!!.messages.single().body)
            assertTrue(c.status,c.status.contains("须先确认云端发送范围"))
            assertNull(c.result); assertTrue(c.charges.isEmpty())
            val current=c.current!!; val observation=current.evidence.single().batchId
            compose.runOnUiThread { c.confirmLatest(LatestReview(observation,current.scope,listOf("合成：明天八点见"),true,true,true,true)) }; ready()
            assertEquals(1,c.current!!.messages.size); assertTrue(c.status.contains("已经入库")); assertTrue(c.charges.isEmpty())
            compose.runOnUiThread { c.createContact("另一位合成观众") }; ready()
            field("对方最新消息 1（原文）").assertTextContains("")
            assertTrue(c.current!!.messages.isEmpty())
            assertFalse(field("对方最新消息 1（原文）").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text.contains("明天八点见"))
        } finally { clean(account) }
    }

    @Test fun settingsDraftPersistsWithoutCloudConsentAndDestructiveActionsNeedName() {
        val account=start()
        try {
            nav("ME"); click("模型与组件")
            click("狗头军师 Skill：默认开启")
            fill("风格补充说明","合成配置草稿：简短自然")
            nav("PROFILE"); nav("ME"); click("模型与组件")
            node("狗头军师 Skill：默认开启").assertIsOff()
            field("风格补充说明").assertTextContains("合成配置草稿：简短自然")
            compose.activityRule.scenario.recreate(); ready()
            field("风格补充说明").assertTextContains("合成配置草稿：简短自然")
            click("保存本主播配置"); ready()
            assertFalse(c.settings.goutouEnabled); assertTrue(c.settings.lianaiEnabled)
            assertFalse(c.settings.jevEnabled); assertFalse(c.settings.cloudConsent)
            compose.onNodeWithContentDescription("返回我的").performClick()
            click("微信前台读取"); node("无障碍服务未连接").assertExists()
            compose.onNodeWithContentDescription("返回我的").performClick()
            click("费用与预算"); node("本设备月度预算",true).assertExists()
            compose.onNodeWithContentDescription("返回我的").performClick()
            click("使用指南"); node("用真实材料，聊出你的语气").assertExists()
            compose.onNodeWithContentDescription("返回我的").performClick()
            click("资料管理"); click("删除此观众")
            node("永久删除").assertIsNotEnabled()
            fill("输入名称确认","另一个名字"); node("永久删除").assertIsNotEnabled()
            click("取消"); assertEquals("合成观众",c.current!!.contact.name); assertTrue(c.charges.isEmpty())
        } finally { clean(account) }
    }

    @Test fun savedCandidateCanBeEditedAndCopiedWithoutLearningOrSending() {
        val account=start()
        try {
            val current=c.current!!
            val analysis=AnalysisFormat.parse("""{"analysis":{"facts":[],"hypotheses":[],"unknowns":["合成UI夹具，非AI结果"],"emotion":"合成","relationship":"合成","disagreements":[],"strategy":"合成UI验证","next_steps":[],"history_limits":"无真实历史"},"no_reply":false,"no_reply_reason":"","candidates":[{"id":"synthetic-c1","style":"合成候选 · 界面测试","segments":["合成：那早点休息呀","合成：明天再聊"],"rationale":"只用于验证界面编辑和复制","risks":[],"scores":{"natural":0,"style_fit":0,"segmentation":0,"length_fit":0,"context_fit":0},"evidence_ids":[]}],"new_topics":[],"suggested_tags":[]}""",emptySet())
            val round=RoundResult(scope=current.scope,startedAt=System.currentTimeMillis(),model="SYNTHETIC_UI_ONLY",coverage=Coverage(0,0,"synthetic",emptyList(),emptyList(),0,"非AI质量证据"),components=emptyList(),memories=emptyList(),analysis=analysis)
            compose.runOnUiThread { c.vault.saveRound(round); c.selectContact(current) }; ready()
            click("已保存的分析"); click("分析记录：请选择")
            click("SYNTHETIC_UI_ONLY",true); click("选择并编辑")
            fill("第1条","合成：修改后的草稿")
            compose.onAllNodesWithText("复制这一条")[0].performScrollTo().performClick()
            compose.runOnUiThread { assertEquals("合成：修改后的草稿",compose.activity.getSystemService(ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString()) }
            click("保存草稿并收起")
            assertTrue(c.current!!.messages.isEmpty()); assertTrue(c.charges.isEmpty())
            click("记录实际发送内容"); fill("微信已发送的最终原文","合成：测试人员确认已发送")
            node("确认记录并学习").assertIsNotEnabled()
            click("我已实际发送，并确认原文一致"); click("确认记录并学习"); ready()
            assertEquals(c.status,1,c.current!!.messages.size)
            assertTrue(c.current!!.messages.single().actualSent)
            assertEquals("合成：测试人员确认已发送",c.current!!.messages.single().body)
        } finally { clean(account) }
    }

    @Test fun interfaceEditorKeepsPrivateDraftAcrossRotationAndRequiresPriceReview() {
        val account=start()
        try {
            nav("ME"); click("模型与组件"); click("添加接口配置")
            click("保存接口"); node("请填写配置名称").assertExists()
            fill("配置名称","合成接口 · 不访问网络")
            assertEquals("Immediately after name entry","合成接口 · 不访问网络",c.interfaceDraft.value.name)
            fill("完整HTTPS请求地址","https://invalid.example/chat/completions")
            fill("模型标识","synthetic-model")
            fill("API Key","SYNTHETIC_TEST_CREDENTIAL")
            compose.waitForIdle()
            assertEquals("Before recreation", "SYNTHETIC_TEST_CREDENTIAL",c.interfaceDraft.value.apiKey)
            compose.activityRule.scenario.recreate(); ready()
            assertTrue(c.interfaceEditorOpen)
            assertEquals("SYNTHETIC_TEST_CREDENTIAL",c.interfaceDraft.value.apiKey)
            assertEquals("synthetic-model",c.interfaceDraft.value.model)
            assertEquals("合成接口 · 不访问网络",c.interfaceDraft.value.name)
            click("核验并保存接口")
            compose.waitUntil(5_000) { compose.onAllNodesWithText("调用前须核验人民币计价",substring=true).fetchSemanticsNodes().isNotEmpty() }
            node("调用前须核验人民币计价",true).assertExists()
            fill("输入价（元 / 百万token）","0.01")
            fill("输出价（元 / 百万token）","0.02")
            fill("价格来源、核对日期及汇率依据","SYNTHETIC_UI_ONLY，不是服务商价格")
            click("我已核对人民币价格与模型容量"); click("核验并保存接口")
            assertFalse(c.interfaceEditorOpen); assertEquals("",c.interfaceDraft.value.apiKey)
            assertEquals(1,c.settingsDraft.value.profiles.size); assertTrue(c.settings.profiles.isEmpty())
            click("保存本主播配置"); ready()
            assertEquals(c.settings.profiles.single().id,c.settings.modelId)
            assertFalse(c.settings.cloudConsent); assertTrue(c.charges.isEmpty())
        } finally { clean(account) }
    }

    @Test fun selectingOcrLinesNeverAutoConfirmsIdentityOrCallsCloud() {
        val account=start()
        try {
            compose.runOnUiThread { ReadBridge.preview=Captured(c.current!!.scope,"合成聊天标题\n合成最新消息甲\n合成最新消息乙\n发送",byteArrayOf(),"SYNTHETIC_UI_ONLY",System.currentTimeMillis(),automatic=true) }
            compose.waitForIdle(); click("选择识别文字")
            click("合成最新消息甲"); click("合成最新消息乙"); click("填入核对表")
            field("对方最新消息 1（原文）").assertTextContains("合成最新消息甲")
            field("对方最新消息 2（原文）").assertTextContains("合成最新消息乙")
            node("属于当前观众 合成观众").assertIsOff()
            node("均为对方原文，正文与说话人已核对").assertIsOff()
            node("确认最新消息并分析").assertIsNotEnabled()
            assertTrue(c.current!!.messages.isEmpty()); assertTrue(c.charges.isEmpty()); assertFalse(c.generating)
        } finally { clean(account) }
    }
}

