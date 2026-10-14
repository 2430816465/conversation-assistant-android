package cn.xindong.junshi

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.xindong.core.*
import org.json.JSONObject

@Composable internal fun Advisor(c: ProductController,openHistory: ()->Unit,openModel: ()->Unit) {
    val conversation=c.current ?: return
    var query by rememberSaveable { mutableStateOf("请根据全部可用历史判断现在是否需要回复，提供适合我的口吻的候选。") }
    var draft by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showDraft by remember { mutableStateOf(false) }
    var actual by rememberSaveable { mutableStateOf("") }
    var actualConfirmed by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable(c.result?.id) { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Eyebrow("你的专属聊天参谋")
                Text("让回复\n更像你。",style=MaterialTheme.typography.headlineLarge)
                Text("读懂对方，保留自己的表达。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ConversationArt(Modifier.size(112.dp))
        }
        c.result?.let { round ->
            BoxCard(if(round.analysis.noReply) "这次可以不回复" else "为你准备了 ${round.analysis.candidates.size} 组回复") {
                if(round.analysis.noReply) Hint(round.analysis.noReplyReason)
                round.analysis.candidates.forEachIndexed { index,candidate ->
                    CandidateCard(index,candidate.style,candidate.segments,candidate.rationale,candidate.risks) { draft=candidate.segments; showDraft=true }
                }
                TextButton(onClick={ expanded=!expanded }) { Text(if(expanded) "收起军师分析" else "展开完整军师分析") }
                if(expanded) ReportView(c,round.json())
                Text("本轮提交 ${round.coverage.processedMessages}/${round.coverage.availableMessages} 条可用历史。模型自评不等于人工验收。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                round.analysis.raw.objects("suggested_tags").forEach { t -> OutlinedButton(onClick={
                    c.mutate { old -> old.copy(tags=old.tags+Tag(scope=old.scope,text=t.getString("text"),origin="AI_CONFIRMED_BY_USER",confirmed=true,evidenceMessageIds=t.strings("evidence_ids"))) }
                }) { Text("确认标签：${t.getString("text")}") } }
            }
        }
        if(draft.isNotEmpty()) BoxCard("你的草稿 · 尚未发送") {
            draft.forEach { Text(it) }
            Actions { Button(onClick={ showDraft=true }) { Text("继续编辑与复制") }; TextButton(onClick={ draft=emptyList(); showDraft=false }) { Text("清空草稿") } }
        }
        LatestMessageCard(c)
        if(conversation.messages.isEmpty()) MenuEntry("提供首次聊天记录","补充你们的过往，让建议更有依据",Glyph.IMPORT,openHistory)
        if(c.settings.profiles.none { it.id==c.settings.modelId } || !c.settings.cloudConsent || !c.settings.componentNoticeAccepted) {
            MenuEntry("完成模型配置",if(c.settings.profiles.isEmpty()) "添加自己的API接口，阅读并决定云端授权" else "模型已保存；生成前请检查云端授权与组件说明",Glyph.SPARK,openModel)
        }
        Disclosure("基于现有历史分析",summary="已确认 ${conversation.messages.size} 条 · 待校对 ${conversation.batches.count { !it.committed }} 批 · 媒体 ${conversation.media.size} 份") {
            Text("历史缺口：${conversation.contact.gaps}",style=MaterialTheme.typography.bodyMedium)
            Field("本轮需求",query,{ query=it },lines=2)
            Text("狗头军师 ${if(c.settings.goutouEnabled) "开启" else "关闭"} · 恋爱大师 ${if(c.settings.lianaiEnabled) "开启" else "关闭"} · Jev ${if(c.settings.jevEnabled) "开启" else "关闭"}",style=MaterialTheme.typography.bodySmall)
            Button(onClick={ draft=emptyList(); c.generate(query) },enabled=!c.busy && !c.generating && conversation.messages.isNotEmpty(),modifier=Modifier.fillMaxWidth()) { Text("分析并生成候选") }
        }
        if(c.generating || c.components.isNotEmpty()) BoxCard("本轮进度") {
            c.components.forEach { Text("${it.name} · ${it.state}${if(it.detail.isBlank()) "" else "\n"+it.detail}",style=MaterialTheme.typography.bodySmall) }
            if(c.generating) OutlinedButton(onClick=c::cancel) { Text("取消本轮") }
        }
        Disclosure("记录实际发送内容",summary="微信发送后记录最终原文，作为你的真实口吻样本。") {
            Text("一次记录一条。选择或复制候选不会自动进入学习。",style=MaterialTheme.typography.bodySmall)
            Field("微信已发送的最终原文",actual,{ actual=it; actualConfirmed=false },lines=2)
            Check("我已实际发送，并确认原文一致",actualConfirmed,{ actualConfirmed=it })
            Button(onClick={ c.actualSent(actual); actual=""; actualConfirmed=false },enabled=actualConfirmed && actual.isNotBlank() && !c.busy && !c.generating,modifier=Modifier.fillMaxWidth()) { Text("确认记录并学习") }
        }
        if(c.rounds.isNotEmpty()) SavedReports(c) { draft=it; showDraft=true }
    }
    if(showDraft && draft.isNotEmpty()) DraftSheet(draft,{ draft=it },{ showDraft=false })
}

@Composable private fun CandidateCard(index: Int,style: String,segments: List<String>,rationale: String,risks: List<String>,choose: ()->Unit) {
    Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow,modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Surface(Modifier.size(28.dp),shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment=Alignment.Center) { Text("${index+1}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onPrimaryContainer) }
                }
                Text(style,style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary)
            }
            segments.forEach { SelectionContainer { Text(it,style=MaterialTheme.typography.bodyLarge) } }
            if(segments.size>1) Text("分 ${segments.size} 条发送",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(rationale.isNotBlank()) Text(rationale,style=MaterialTheme.typography.bodySmall)
            if(risks.isNotEmpty()) Text("留意："+risks.joinToString("；"),style=MaterialTheme.typography.bodySmall)
            FilledTonalButton(onClick=choose,modifier=Modifier.align(Alignment.End)) { Text("选择并编辑"); Spacer(Modifier.width(8.dp)); AppIcon(Glyph.ARROW) }
        }
    }
}

@Composable internal fun LatestMessageCard(c: ProductController) {
    val conversation=c.current ?: return; val context=LocalContext.current
    val capture=ReadBridge.preview?.takeIf { it.scope==conversation.scope }
    val reviewKey=capture?.id ?: ("paste-"+(conversation.messages.maxOfOrNull { it.order } ?: 0))
    var observation by rememberSaveable(reviewKey) { mutableStateOf(capture?.id ?: newId()) }
    var bodies by rememberSaveable(reviewKey) { mutableStateOf(listOf("")) }
    var identity by rememberSaveable(reviewKey) { mutableStateOf(false) }
    var incoming by rememberSaveable(reviewKey) { mutableStateOf(false) }
    var latest by rememberSaveable(reviewKey) { mutableStateOf(false) }
    var selectOcr by remember { mutableStateOf(false) }
    var showRaw by rememberSaveable(reviewKey) { mutableStateOf(false) }
    var clipboardNotice by remember { mutableStateOf("") }
    BoxCard("对方说了什么？") {
        Text("放入最新消息，一起想想怎么回。",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
        if(capture!=null) {
            Hint("已有${if(capture.automatic) "自动" else "手动"}读取。文字、归属和顺序仍需你核对。")
            Actions { OutlinedButton(onClick={ selectOcr=true }) { Text("选择识别文字") }; TextButton(onClick={ showRaw=!showRaw }) { Text(if(showRaw) "收起读取原文" else "查看读取原文") } }
            if(showRaw) SelectionContainer { Text(capture.text,style=MaterialTheme.typography.bodyMedium) }
        }
        bodies.forEachIndexed { i,body ->
            Field("对方最新消息 ${i+1}（原文）",body,{ value -> bodies=bodies.mapIndexed { j,s -> if(i==j) value else s }; incoming=false; latest=false },lines=3)
            if(bodies.size>1) TextButton(onClick={ bodies=bodies.filterIndexed { j,_ -> i!=j }; incoming=false; latest=false }) { Text("移除第${i+1}条") }
        }
        Actions {
            TextButton(onClick={
                val text=context.getSystemService(ClipboardManager::class.java).primaryClip?.takeIf { it.itemCount>0 }?.getItemAt(0)?.coerceToText(context)?.toString()
                if(!text.isNullOrBlank()) { bodies=bodies.dropLast(1)+text; incoming=false; latest=false; clipboardNotice="已粘贴到最后一条，请核对。" }
                else clipboardNotice="剪贴板没有可用文字。"
            }) { AppIcon(Glyph.COPY); Spacer(Modifier.width(6.dp)); Text("粘贴原文") }
            TextButton(onClick={ bodies=bodies+""; incoming=false; latest=false }) { AppIcon(Glyph.PLUS); Spacer(Modifier.width(6.dp)); Text("添加一条") }
        }
        if(clipboardNotice.isNotBlank()) Text(clipboardNotice,style=MaterialTheme.typography.bodySmall)
        if(bodies.any { it.isNotBlank() }) {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Eyebrow("分析前，请确认")
            Check("属于当前观众 ${conversation.contact.name}",identity,{ identity=it })
            Check("均为对方原文，正文与说话人已核对",incoming,{ incoming=it })
            Check("按顺序排列的最新未回复消息，尚未入库",latest,{ latest=it })
        }
        Button(onClick={ c.confirmLatest(LatestReview(observation,conversation.scope,bodies,identity,incoming,latest,latest),capture) },
            enabled=identity && incoming && latest && bodies.all { it.isNotBlank() } && !c.busy && !c.generating,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)) {
            AppIcon(Glyph.SPARK); Spacer(Modifier.width(8.dp)); Text("确认最新消息并分析")
        }
        if(bodies.any { it.isNotBlank() }) {
            Text("确认后保存原文并调用云端模型，按服务商计费。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={ if(capture!=null) { ReadBridge.consumedId=capture.id; ReadBridge.preview=null }; observation=newId(); bodies=listOf(""); identity=false; incoming=false; latest=false }) { Text("清空本次核对") }
        } else {
            Text("仅分析已提供的历史 · 读取设置在「我的」",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if(selectOcr && capture!=null) OcrSelectionDialog(capture.text,{ selectOcr=false }) {
        bodies=it; identity=false; incoming=false; latest=false; selectOcr=false
    }
}

@Composable private fun OcrSelectionDialog(text: String,close: ()->Unit,selected: (List<String>)->Unit) {
    val lines=remember(text) { text.lines().map { it.trim() }.filter { it.isNotBlank() } }
    var indexes by remember(text) { mutableStateOf(setOf<Int>()) }
    AlertDialog(onDismissRequest=close,title={ Text("选出对方最新消息") },text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Hint("只选对方最新文字，跳过标题、时间、按钮和主播消息。OCR可能把一条拆成多行，填入后可合并修正。")
            lines.forEachIndexed { index,line -> Check(line,index in indexes) { enabled -> indexes=if(enabled) indexes+index else indexes-index } }
        }
    },confirmButton={ TextButton(onClick={ selected(lines.filterIndexed { index,_ -> index in indexes }) },enabled=indexes.isNotEmpty()) { Text("填入核对表") } },dismissButton={ TextButton(onClick=close) { Text("取消") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun DraftSheet(draft: List<String>,change: (List<String>)->Unit,close: ()->Unit) {
    val context=LocalContext.current; var copied by remember { mutableStateOf<Int?>(null) }
    ModalBottomSheet(onDismissRequest=close) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("你的草稿",style=MaterialTheme.typography.headlineSmall)
            Text("修改、逐条复制，由你在微信发送。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            draft.forEachIndexed { index,body ->
                Field("第${index+1}条",body,{ value -> change(draft.mapIndexed { i,s -> if(i==index) value else s }); copied=null },lines=2)
                Actions {
                    Button(onClick={ context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("军师草稿",draft[index])); copied=index },enabled=body.isNotBlank()) { Text(if(copied==index) "已复制这一条" else "复制这一条") }
                    TextButton(onClick={ change(draft.filterIndexed { i,_ -> i!=index }); if(draft.size==1) close() }) { Text("移除") }
                }
            }
            TextButton(onClick={ change(draft+"") }) { Text("增加一条") }
            OutlinedButton(onClick=close,modifier=Modifier.fillMaxWidth()) { Text("保存草稿并收起") }
        }
    }
}

@Composable private fun SavedReports(c: ProductController,choose: (List<String>)->Unit) {
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    Disclosure("已保存的分析",summary="${c.rounds.size} 份记录，可查看候选、分析和原文证据。") {
        Pick("分析记录",c.rounds,selected,{ java.time.Instant.ofEpochMilli(it.getLong("startedAt")).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))+" · "+it.getString("model") },{ selected=it })
        selected?.let { round ->
            val result=round.getJSONObject("result")
            if(result.optBoolean("no_reply")) Hint(result.optString("no_reply_reason"))
            result.objects("candidates").forEachIndexed { index,candidate ->
                CandidateCard(index,candidate.optString("style"),candidate.strings("segments"),candidate.optString("rationale"),candidate.strings("risks")) { choose(candidate.strings("segments")) }
            }
            ReportView(c,round)
        }
    }
}
