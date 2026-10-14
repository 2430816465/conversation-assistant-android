package cn.xindong.junshi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cn.xindong.core.*
import org.json.JSONObject
import java.util.Locale

@Composable internal fun HistoryScreen(c: ProductController, textFile: ()->Unit, screenshots: ()->Unit, media: ()->Unit) {
    val conversation=c.current ?: return; val context=LocalContext.current; var paste by rememberSaveable { mutableStateOf("") }; var batchId by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableIntStateOf(0) }; var editId by remember { mutableStateOf<String?>(null) }
    var method by rememberSaveable { mutableStateOf("粘贴文字") }; var search by rememberSaveable { mutableStateOf("") }
    var pendingPasteEvidenceCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(conversation.evidence.size) {
        pendingPasteEvidenceCount?.let { if(conversation.evidence.size>it) { paste=""; pendingPasteEvidenceCount=null } }
    }
    var evidence by remember { mutableStateOf<Evidence?>(null) }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionHeading("你们的聊天","把过往补充进来，让建议更有依据。")
        BoxCard("1 选择导入方式") {
            Actions { listOf("粘贴文字","截图OCR","文件与媒体").forEach { label -> FilterChip(selected=method==label,onClick={ method=label },label={ Text(label) }) } }
            when(method) {
                "粘贴文字" -> {
                    Text("每条消息另起一行，导入后再核对说话人与顺序。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Field("微信复制文字或逐行消息",paste,{ paste=it },lines=4)
                    TextButton(onClick={
                        val clip=context.getSystemService(ClipboardManager::class.java).primaryClip
                        clip?.takeIf { it.itemCount>0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.let { paste=it }
                    }) { AppIcon(Glyph.COPY); Spacer(Modifier.width(8.dp)); Text("从剪贴板粘贴") }
                    Button(onClick={ pendingPasteEvidenceCount=conversation.evidence.size; c.importText(paste) },enabled=paste.isNotBlank() && !c.busy,modifier=Modifier.fillMaxWidth()) { Text("导入待校对区") }
                    Text("结构化文字也支持：时间、主播/观众、TEXT、正文，以制表符分隔。未知时间可留空。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "截图OCR" -> {
                    Text("选择已授权聊天的多张截图，在下一步逐条修正OCR。按系统返回顺序加入，须核对为从旧到新；重叠内容由你决定是否跳过。")
                    Button(onClick=screenshots,enabled=!c.busy,modifier=Modifier.fillMaxWidth()) { Text("批量截图OCR") }
                }
                else -> {
                    Text("文字文件支持UTF-8 JSON/TSV；媒体原件本地加密保存，完整转写或描述由你核对。")
                    OutlinedButton(onClick=textFile,enabled=!c.busy,modifier=Modifier.fillMaxWidth()) { Text("导入文字文件") }
                    OutlinedButton(onClick=media,enabled=!c.busy,modifier=Modifier.fillMaxWidth()) { Text("导入图片、语音、视频或表情") }
                }
            }
        }
        if(conversation.batches.isNotEmpty()) BoxCard("2 校对导入批次") {
            conversation.batches.forEach { b -> OutlinedButton(onClick={ batchId=b.id },enabled=!b.committed) { Text("${b.id.take(6)} · ${b.rows.size}行 · ${if(b.committed) "已确认" else "待校对"}") } }
            batchId?.let { id -> conversation.batches.firstOrNull { it.id==id && !it.committed }?.let { batch -> key(batch.id) { BatchEditor(c,batch) { batchId=null } } } }
        }
        BoxCard("3 已确认历史 · ${conversation.messages.size}条") {
            if(conversation.messages.isEmpty()) Text("还没有已确认消息。上方导入并完成校对后，会显示在这里。",color=MaterialTheme.colorScheme.onSurfaceVariant)
            else Field("查找历史原文",search,{ search=it; page=0 })
            val sorted=conversation.messages.filter { it.body.contains(search,ignoreCase=true) }.sortedBy { it.order }; val pages=maxOf(1,(sorted.size+19)/20); page=page.coerceIn(0,pages-1)
            if(search.isNotBlank()) Text("找到 ${sorted.size} 条；查找只影响展示，分析仍使用全部已确认历史。",style=MaterialTheme.typography.bodySmall)
            if(conversation.messages.isNotEmpty()) {
                OutlinedButton(onClick={ c.mutate { old -> old.copy(messages=old.messages.sortedWith(compareBy<Message> { java.time.OffsetDateTime.parse(it.time).toInstant() }.thenBy { it.order }).mapIndexed { i,m -> m.copy(order=i+1L) }) } },enabled=conversation.messages.all { it.time.isNotBlank() && HistoryImport.validTime(it.time) } && !c.busy) { Text("按全部已核对时间整理顺序") }
                Actions { TextButton(onClick={ page-- },enabled=page>0) { Text("上一页") }; Text("${page+1}/$pages",Modifier.padding(top=14.dp)); TextButton(onClick={ page++ },enabled=page<pages-1) { Text("下一页") } }
            }
            sorted.drop(page*20).take(20).forEach { m ->
                Text("#${m.order} ${speakerLabel(m.speaker)} · ${m.time.ifBlank { "时间未知" }} · ${kindLabel(m.kind)}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape=MaterialTheme.shapes.medium,color=if(m.speaker==Speaker.STREAMER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                    SelectionContainer { Text(m.body,Modifier.fillMaxWidth().padding(14.dp),style=MaterialTheme.typography.bodyLarge) }
                }
                Text("修订${m.revision} · ${if(m.actualSent) "主播已发送确认" else "观众材料"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(m.actualSent && m.speaker==Speaker.STREAMER && m.kind in listOf(Kind.TEXT,Kind.EMOJI)) TextButton(onClick={ c.mutate { old -> old.copy(messages=old.messages.map { if(it.id==m.id) it.copy(styleExample=!it.styleExample) else it }) } },enabled=!c.busy) { Text(if(m.styleExample) "移出精选风格示例" else "选为本观众的风格示例") }
                Actions { TextButton(onClick={ editId=m.id }) { Text("校正") }; m.evidenceIds.forEachIndexed { index,id -> TextButton(onClick={ evidence=conversation.evidence.single { it.id==id } }) { Text("来源${index+1}") } }
                    TextButton(onClick={ c.mutate { old -> val list=old.messages.sortedBy { it.order }; val i=list.indexOfFirst { it.id==m.id }; require(i>0); val before=list[i-1]; old.copy(messages=old.messages.map { when(it.id) { m.id -> it.copy(order=before.order); before.id -> it.copy(order=m.order); else -> it } }) } },enabled=sorted.firstOrNull()?.id!=m.id && !c.busy) { Text("上移") }
                }; HorizontalDivider()
            }
        }
        if(conversation.media.isNotEmpty()) BoxCard("媒体原件与完整描述/转写") {
            conversation.media.forEach { asset -> key(asset.id) { MediaEditor(c,asset) } }
        }
    }
    editId?.let { id -> conversation.messages.firstOrNull { it.id==id }?.let { m ->
        var body by remember(id) { mutableStateOf(m.body) }; var speaker by remember(id) { mutableStateOf(m.speaker) }; var time by remember(id) { mutableStateOf(m.time) }
        AlertDialog(onDismissRequest={ editId=null },title={ Text("校正消息，原始证据保留") },text={ Column(Modifier.verticalScroll(rememberScrollState())) { Field("正文",body,{ body=it },lines=3); Pick("说话人",listOf(Speaker.STREAMER,Speaker.CONTACT),speaker,::speakerLabel,{ speaker=it }); MessageTime(time) { time=it } } },
            confirmButton={ TextButton(onClick={ c.mutate { old -> old.copy(messages=old.messages.map { if(it.id==id) it.copy(body=body,speaker=speaker,time=time,actualSent=speaker==Speaker.STREAMER,revision=it.revision+1,correctedFrom=JSONObject().put("previous_body",it.body).put("previous_speaker",it.speaker.name).put("previous_time",it.time).put("previous_revision_chain",it.correctedFrom).toString(),reviewedAt=System.currentTimeMillis()) else it }) }; editId=null },enabled=body.isNotBlank() && HistoryImport.validTime(time)) { Text("已核对，保存校正") } },dismissButton={ TextButton(onClick={ editId=null }) { Text("取消") } })
    } }
    evidence?.let { e -> AlertDialog(onDismissRequest={ evidence=null },title={ Text("原始来源 · ${e.kind}") },text={ Column { SelectionContainer { Text(e.raw) }; Text("SHA256 ${e.sha256}",style=MaterialTheme.typography.bodySmall); e.blobId?.let { SourceImage(c,e.scope,it) } } },confirmButton={ TextButton(onClick={ evidence=null }) { Text("关闭") } }) }
}

@Composable internal fun BatchEditor(c: ProductController, original: ImportBatch, close: ()->Unit) {
    var batch by c.reviewDraft(original); var page by rememberSaveable { mutableIntStateOf(0) }
    fun row(index: Int, change: (DraftRow)->DraftRow) { batch=batch.copy(rows=batch.rows.mapIndexed { i,r -> if(i==index) change(r) else r }) }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("本批 ${batch.rows.size}行；OCR可能包含标题、时间和按钮，须跳过这些控件。")
        Check("已逐项核对本批确属当前观众 ${c.current?.contact?.name}",batch.identityConfirmed,{ batch=batch.copy(identityConfirmed=it) })
        Check("已核对本批从旧到新的顺序；并确认接在现有历史后，较早材料入库后可上移",batch.orderConfirmed,{ batch=batch.copy(orderConfirmed=it) })
        Field("本批缺口与说明",batch.gaps,{ batch=batch.copy(gaps=it) })
        val pages=maxOf(1,(batch.rows.size+9)/10); page=page.coerceIn(0,pages-1)
        Actions { TextButton(onClick={ page-- },enabled=page>0) { Text("上一组") }; Text("${page+1}/$pages",Modifier.padding(top=14.dp)); TextButton(onClick={ page++ },enabled=page<pages-1) { Text("下一组") } }
        batch.rows.drop(page*10).take(10).forEachIndexed { offset,r -> val i=page*10+offset
            key(r.id) {
                Text("第${i+1}行"+(r.overlapWith?.let { " · 可能重叠：${it.take(8)}，请自行核对" } ?: ""),fontWeight=FontWeight.Bold)
                Field("正文",r.body,{ value -> row(i) { it.copy(body=value,checked=false) } },lines=2)
                Pick("说话人",Speaker.entries.toList(),r.speaker,::speakerLabel,{ value -> row(i) { it.copy(speaker=value,checked=false) } })
                Pick("类型",Kind.entries.toList(),r.kind,::kindLabel,{ value -> row(i) { it.copy(kind=value,checked=false) } })
                MessageTime(r.time) { value -> row(i) { it.copy(time=value,checked=false) } }
                Check("跳过这行（控件、重叠或不属于消息）",r.skip,{ value -> row(i) { it.copy(skip=value) } })
                Check("已核对正文、说话人及时间；主播行确认为实际已发送",r.checked,{ value -> row(i) { it.copy(checked=value) } })
                Actions {
                    TextButton(onClick={ val rows=batch.rows.toMutableList(); val previous=rows[i-1]; rows[i-1]=rows[i]; rows[i]=previous; batch=batch.copy(rows=rows,orderConfirmed=false) },enabled=i>0) { Text("上移") }
                    TextButton(onClick={ batch=batch.copy(rows=HistoryImport.merge(batch.rows,i),orderConfirmed=false) },enabled=i<batch.rows.lastIndex) { Text("与下一行合为一条") }
                }; HorizontalDivider()
            }
        }
        OutlinedButton(onClick={ c.updateBatch(batch) },modifier=Modifier.fillMaxWidth(),enabled=!c.busy) { Text("保存校对进度") }
        Button(onClick={ c.commitBatch(batch); close() },modifier=Modifier.fillMaxWidth(),enabled=!c.busy && batch.identityConfirmed && batch.orderConfirmed && batch.rows.any { !it.skip } && batch.rows.filterNot { it.skip }.all { it.checked && it.speaker!=Speaker.UNKNOWN && it.body.isNotBlank() && HistoryImport.validTime(it.time) }) { Text("确认入库") }
        TextButton(onClick=close) { Text("收起") }
    }
}

@Composable internal fun SourceImage(c: ProductController, scope: Scope, blob: String) {
    val context=LocalContext.current; var image by remember(blob) { mutableStateOf<ImageBitmap?>(null) }; var state by remember(blob) { mutableStateOf("原件保存在本地加密仓库") }
    OutlinedButton(onClick={
        (context.applicationContext as JunshiApp).io.execute {
            try { val bytes=c.vault.blob(scope,blob); val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }; BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                val options=BitmapFactory.Options().apply { inSampleSize=maxOf(1,maxOf(bounds.outWidth,bounds.outHeight)/1600) }
                val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
                android.os.Handler(android.os.Looper.getMainLooper()).post { image=bitmap?.asImageBitmap(); state=if(bitmap==null) "此原件需导出后使用相应音视频播放器核对" else "本地原件预览，缩放不改变保存文件" }
            } catch(e: Exception) { android.os.Handler(android.os.Looper.getMainLooper()).post { state=AnalysisEngine.safeError(e) } }
        }
    }) { Text("查看本地原件") }
    Text(state,style=MaterialTheme.typography.bodySmall); image?.let { Image(it,"授权原始材料",Modifier.fillMaxWidth().heightIn(max=360.dp)) }
}
@Composable internal fun MediaEditor(c: ProductController, m: MediaAsset) {
    var playing by remember(m.id) { mutableStateOf(false) }
    var description by remember(m.id,m.description) { mutableStateOf(m.description) }; var checked by remember(m.id,m.verified) { mutableStateOf(m.verified) }
    Text("${m.originalName} · ${m.mime}",fontWeight=FontWeight.Bold); SourceImage(c,m.scope,m.blobId)
    if(m.mime.startsWith("audio/") || m.mime.startsWith("video/")) OutlinedButton(onClick={ playing=true }) { Text("本地播放音视频原件") }
    if(playing) MediaReview(c,m) { playing=false }
    Field("完整文字转写/图片或表情描述；未识别部分写明未知",description,{ description=it; checked=false },lines=3)
    Check("我已核对原件与完整描述/转写",checked,{ checked=it })
    OutlinedButton(onClick={ c.mutate { old -> old.copy(media=old.media.map { if(it.id==m.id) it.copy(description=description,verified=checked) else it }) } },enabled=description.isNotBlank() && !c.busy) { Text("保存媒体说明") }
    HorizontalDivider()
}

@Composable internal fun ProfileScreen(c: ProductController) {
    val old=c.current ?: return; var name by rememberSaveable(old.contact.name) { mutableStateOf(old.contact.name) }; var notes by rememberSaveable(old.contact.notes) { mutableStateOf(old.contact.notes) }
    var boundaries by rememberSaveable(old.contact.boundaries) { mutableStateOf(old.contact.boundaries) }; var gaps by rememberSaveable(old.contact.gaps) { mutableStateOf(old.contact.gaps) }
    var tag by rememberSaveable { mutableStateOf("") }; var factKey by rememberSaveable { mutableStateOf("") }; var factValue by rememberSaveable { mutableStateOf("") }; var factIds by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Avatar(old.contact.name,Modifier.size(64.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(old.contact.name,style=MaterialTheme.typography.headlineSmall)
                Text("记住这个人，也尊重彼此的边界。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.fillMaxWidth().padding(vertical=16.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                listOf(old.messages.size to "聊天记忆",old.tags.size to "偏好标签",old.facts.size to "长期记忆").forEach { (count,label) ->
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(count.toString(),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
        BoxCard("基本资料") {
            Field("显示名称",name,{ name=it }); Field("备注与偏好",notes,{ notes=it },lines=2); Field("明确交流边界",boundaries,{ boundaries=it },lines=2); Field("历史完整度与缺口",gaps,{ gaps=it },lines=2)
            Button(onClick={ c.mutate { it.copy(contact=it.contact.copy(name=name,notes=notes,boundaries=boundaries,gaps=gaps)) } },enabled=name.isNotBlank() && gaps.isNotBlank() && !c.busy,modifier=Modifier.fillMaxWidth()) { Text("保存档案") }
            Text("独立ID ${old.scope.contactId.take(8)} · 同名不会合并",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Disclosure("标签与偏好",summary="${old.tags.size} 个标签。没有标签也可以分析。") {
            old.tags.forEach { t -> var value by remember(t.id,t.text) { mutableStateOf(t.text) }; Field("${t.origin} ${if(t.confirmed) "已确认" else "待确认"}",value,{ value=it })
                Actions { TextButton(onClick={ c.mutate { o -> o.copy(tags=o.tags.map { if(it.id==t.id) it.copy(text=value,confirmed=true) else it }) } },enabled=value.isNotBlank() && !c.busy) { Text("保存确认") }; TextButton(onClick={ c.mutate { o -> o.copy(tags=o.tags.filterNot { it.id==t.id }) } },enabled=!c.busy) { Text("删除标签") } }
            }
            Field("新标签",tag,{ tag=it }); OutlinedButton(onClick={ val value=tag; c.mutate { o -> o.copy(tags=o.tags+Tag(scope=o.scope,text=value)) }; tag="" },enabled=tag.isNotBlank() && !c.busy) { Text("手工添加") }
        }
        Disclosure("长期记忆与矛盾",initiallyOpen=old.conflicts.any { it.resolvedBy==null },summary="${old.facts.size} 条事实 · ${old.conflicts.count { it.resolvedBy==null }} 项待核对矛盾") {
            old.facts.forEach { f -> Text("${f.key}：${f.value}\n来源${f.origin} · ${java.time.Instant.ofEpochMilli(f.at)}\n证据${f.evidenceMessageIds.joinToString()}") }
            old.conflicts.forEach { conflict -> Text("矛盾：${conflict.note}\n记录：${conflict.factIds.joinToString()}\n处理：${conflict.resolvedBy ?: "待核对"}")
                var resolution by remember(conflict.id) { mutableStateOf(conflict.resolvedBy ?: "") }; Field("处理说明（原事实均保留）",resolution,{ resolution=it })
                TextButton(onClick={ c.mutate { o -> o.copy(conflicts=o.conflicts.map { if(it.id==conflict.id) it.copy(resolvedBy=resolution) else it }) } },enabled=resolution.isNotBlank() && !c.busy) { Text("保存矛盾说明") }
            }
            Field("事实字段，例如生日/偏好",factKey,{ factKey=it }); Field("事实内容",factValue,{ factValue=it }); Field("证据消息ID，以英文逗号分隔；手工备注可留空",factIds,{ factIds=it })
            OutlinedButton(onClick={ val k=factKey; val v=factValue; val ids=factIds.split(',').map { it.trim() }.filter { it.isNotBlank() }; c.mutate { o -> o.addFact(Fact(scope=o.scope,key=k,value=v,evidenceMessageIds=ids)) }; factKey=""; factValue=""; factIds="" },enabled=factKey.isNotBlank() && factValue.isNotBlank() && !c.busy) { Text("添加记录，保留不同版本") }
        }
    }
}

@Composable internal fun ReportView(c: ProductController, round: JSONObject) {
    val result=round.getJSONObject("result"); val analysis=result.getJSONObject("analysis"); val current=c.current ?: return
    var source by remember(round.optString("id")) { mutableStateOf<Message?>(null) }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("模型版本：${round.getString("model")}",style=MaterialTheme.typography.bodySmall)
        round.optJSONObject("reply_target")?.let { target -> Text("本轮回复目标（用户已核对）",fontWeight=FontWeight.Bold); target.objects("messages").forEach { Text(it.getString("body")) } }
        listOf("facts" to "事实与原文证据","hypotheses" to "推测（尚未证实）").forEach { (field,label) ->
            Text(label,fontWeight=FontWeight.Bold)
            analysis.objects(field).forEach { point ->
                Text(point.getString("text")); Actions { point.strings("evidence_ids").forEach { id -> TextButton(onClick={ source=current.messages.firstOrNull { it.id==id } }) { Text("回查 ${id.takeLast(8)}") } } }
            }
        }
        listOf("emotion" to "情绪判断","relationship" to "关系与不确定性","strategy" to "建议策略","history_limits" to "历史限制").forEach { (field,label) -> Text(label,fontWeight=FontWeight.Bold); Text(analysis.getString(field)) }
        listOf("unknowns" to "未知信息","disagreements" to "组件分歧与取舍","next_steps" to "下一步建议").forEach { (field,label) -> Text(label,fontWeight=FontWeight.Bold); analysis.strings(field).forEach { Text("• $it") } }
        Text("可选新话题",fontWeight=FontWeight.Bold); result.strings("new_topics").forEach { Text("• $it") }
        if(result.getBoolean("no_reply")) Text("建议不回复：${result.getString("no_reply_reason")}")
        round.objects("components").forEach { component ->
            var show by remember(component.getString("name"),round.optString("id")) { mutableStateOf(false) }
            TextButton(onClick={ show=!show }) { Text("${component.getString("name")} · ${component.getString("state")} · ${if(show) "收起" else "展开原始分析"}") }
            if(show) SelectionContainer { Text(component.optString("content")) }
        }
        val coverage=round.getJSONObject("coverage")
        Text("本轮提交 ${coverage.getInt("processed")}/${coverage.getInt("available")} 条；原文回查 ${coverage.strings("recalledIds").size} 条；分块 ${coverage.getInt("chunks")}；${coverage.getString("mode")}\n${coverage.getString("gaps")}",style=MaterialTheme.typography.bodySmall)
        Text("原始材料与完整覆盖清单随资料导出；提交记录不等于摘要语义验收。",style=MaterialTheme.typography.bodySmall)
    }
    source?.let { m -> AlertDialog(onDismissRequest={ source=null },title={ Text("原文回查 · ${m.speaker}") },text={ Column(Modifier.verticalScroll(rememberScrollState())) { Text(m.id,style=MaterialTheme.typography.bodySmall); Text(m.time.ifBlank { "时间未知" }); SelectionContainer { Text(m.body) }; m.evidenceIds.forEach { id -> current.evidence.firstOrNull { it.id==id }?.let { e -> Text("来源 ${e.kind} · ${e.sha256}",style=MaterialTheme.typography.bodySmall); e.blobId?.let { SourceImage(c,m.scope,it) } } } } },confirmButton={ TextButton(onClick={ source=null }) { Text("关闭") } }) }
}
