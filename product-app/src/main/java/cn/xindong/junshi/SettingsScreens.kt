package cn.xindong.junshi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.xindong.core.*
import java.util.Locale
import kotlinx.coroutines.launch

internal fun yuan(micro: Long)=String.format(Locale.ROOT,"%.6f",micro/1_000_000.0)

@Composable internal fun ModelSettings(c: ProductController,draft: MutableState<AiSettings>) {
    var edited by draft
    var remove by remember { mutableStateOf<ModelConfig?>(null) }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        SectionHeading("选好模型，再开始聊天","接口、组件和云端授权按当前主播保存。")
        if(edited!=c.settings) Hint("有未保存的配置。点击底部“保存本主播配置”后才会用于生成；切换主播或退出会清除未保存内容。")
        BoxCard("生成模型") {
            if(edited.profiles.none { it.kind==ProviderKind.CHAT_COMPLETIONS }) Text("先添加你自己的模型接口。API Key只保存在本机加密仓库。")
            Pick("生成模型",edited.profiles.filter { it.kind==ProviderKind.CHAT_COMPLETIONS },edited.profiles.find { it.id==edited.modelId },{ it.name },{ edited=edited.copy(modelId=it.id) })
            edited.profiles.forEach { profile ->
                HorizontalDivider()
                Text(profile.name,style=MaterialTheme.typography.titleSmall)
                Text("${if(profile.kind==ProviderKind.JEV) "Jev辅助判断" else "生成接口"} · ${profile.model}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Actions { OutlinedButton(onClick={ c.editInterface(profile) }) { Text("编辑 ${profile.name}") }; TextButton(onClick={ remove=profile }) { Text("移除 ${profile.name}") } }
            }
            OutlinedButton(onClick={ c.editInterface() },modifier=Modifier.fillMaxWidth()) { AppIcon(Glyph.PLUS); Spacer(Modifier.width(8.dp)); Text("添加接口配置") }
        }
        BoxCard("军师分析组件") {
            Check("狗头军师 Skill：默认开启",edited.goutouEnabled,{ edited=edited.copy(goutouEnabled=it) })
            Check("恋爱大师 Skill：默认开启",edited.lianaiEnabled,{ edited=edited.copy(lianaiEnabled=it) })
            Text("两个Skill提供不同分析视角，由所选生成模型调用；按MIT许可集成。已开启的组件失败会明确报错。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Check("Jev：可选辅助判断，默认关闭",edited.jevEnabled,{ edited=edited.copy(jevEnabled=it) })
            if(edited.jevEnabled) {
                Pick("Jev接口",edited.profiles.filter { it.kind==ProviderKind.JEV },edited.profiles.find { it.id==edited.jevId },{ it.name },{ edited=edited.copy(jevId=it.id) })
                Hint("Jev需要独立的TypeSafe接口和凭据；不能直接使用生成模型的Key。")
            }
            Pick("分析偏好",JevMode.entries.toList(),edited.jevMode,{ it.label },{ edited=edited.copy(jevMode=it) })
            Text(edited.jevMode.instruction,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BoxCard("主播口吻") {
            Field("风格补充说明",edited.styleNotes,{ edited=edited.copy(styleNotes=it) },lines=3)
            Text("格式统计仍使用全部已确认的主播实际发送记录。缺少真实样本时，不能验证口吻模仿。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BoxCard("云端发送授权") {
            Text("生成会发送当前观众的已校对文字、档案、媒体描述，以及当前主播的格式统计到你配置的服务商。其他观众原句不会发送。")
            Check("我理解分析仅供参考，Jev数值不是心理事实，Skill作者不是主播本人",edited.componentNoticeAccepted,{ edited=edited.copy(componentNoticeAccepted=it) })
            Check("允许在我点击生成或确认最新消息后，将上述材料发送至已配置的云端接口",edited.cloudConsent,{ edited=edited.copy(cloudConsent=it) })
            Text("本地导入和整理不要求云端授权。保存设置本身不会调用AI。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick={ c.saveSettings(edited) },enabled=!c.busy && !c.generating,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("保存本主播配置") }
    }
    if(c.interfaceEditorOpen) ModelEditor(c.interfaceDraft,c::closeInterface) { config ->
        val firstGeneration=edited.profiles.none { it.kind==ProviderKind.CHAT_COMPLETIONS } && config.kind==ProviderKind.CHAT_COMPLETIONS
        val firstJev=edited.profiles.none { it.kind==ProviderKind.JEV } && config.kind==ProviderKind.JEV
        edited=edited.copy(profiles=edited.profiles.filterNot { it.id==config.id }+config,
            modelId=if(firstGeneration) config.id else edited.modelId,jevId=if(firstJev) config.id else edited.jevId)
        c.closeInterface()
    }
    remove?.let { profile -> AlertDialog(onDismissRequest={ remove=null },title={ Text("移除此接口？") },text={ Text("将从当前配置草稿移除“${profile.name}”。保存本主播配置后生效，聊天和报告会保留。") },
        confirmButton={ TextButton(onClick={ edited=edited.copy(profiles=edited.profiles.filterNot { it.id==profile.id },modelId=if(edited.modelId==profile.id) "" else edited.modelId,jevId=if(edited.jevId==profile.id) "" else edited.jevId); remove=null }) { Text("移除") } },dismissButton={ TextButton(onClick={ remove=null }) { Text("取消") } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ModelEditor(draft: MutableState<ModelConfig>,close: ()->Unit,save: (ModelConfig)->Unit) {
    var config by draft
    var error by remember { mutableStateOf<String?>(null) }
    val listState=rememberLazyListState(); val scope=rememberCoroutineScope(); val focus=LocalFocusManager.current
    fun checkedSave() { try { require(config.name.isNotBlank()) { "请填写配置名称" }; config.validate(); save(config) } catch(e: Exception) { error=AnalysisEngine.safeError(e); focus.clearFocus(); scope.launch { listState.scrollToItem(0) } } }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        JunshiTheme {
            Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={
                TopAppBar(title={ Text("接口配置",maxLines=1,overflow=TextOverflow.Ellipsis) },navigationIcon={ IconButton(onClick=close) { AppIcon(Glyph.BACK,"取消接口编辑") } },
                    actions={ TextButton(onClick={ checkedSave() }) { Text("保存接口") } },colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))
            }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),contentAlignment=Alignment.TopCenter) {
                    LazyColumn(Modifier.widthIn(max=720.dp).fillMaxWidth().testTag("model-editor-scroll"),state=listState,verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(20.dp)) {
                        item(key="validation") { error?.let { Hint(it,error=true) } }
                        item(key="interface") { BoxCard("接口与模型") {
                            Field("配置名称",config.name,{ config=config.copy(name=it); error=null })
                            Pick("接口类型",ProviderKind.entries.toList(),config.kind,{ if(it==ProviderKind.JEV) "Jev / TypeSafe" else "兼容 Chat Completions" },{
                                config=config.copy(kind=it,endpoint=if(it==ProviderKind.JEV) "https://api.typesafe.ai/v1/systemone" else "https://api.deepseek.com/chat/completions",model=if(it==ProviderKind.JEV) "jev-latest" else "")
                            })
                            Field("完整HTTPS请求地址",config.endpoint,{ config=config.copy(endpoint=it.trim()); error=null },keyboard=KeyboardType.Uri)
                            Field("模型标识",config.model,{ config=config.copy(model=it.trim()); error=null })
                            Field("API Key",config.apiKey,{ config=config.copy(apiKey=it.trim()); error=null },secret=true)
                            Text("模型标识、地址和价格请以服务商当前资料为准。保存只做本地检查，不发送测试请求。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        } }
                        item(key="capacity") { BoxCard("模型容量") {
                            var contextTokens by remember { mutableStateOf(config.contextTokens.toString()) }
                            var output by remember { mutableStateOf(config.outputTokens.toString()) }
                            Field("上下文容量（token）",contextTokens,{ contextTokens=it; config=config.copy(contextTokens=it.toIntOrNull() ?: 0) },keyboard=KeyboardType.Number)
                            Field("最大输出及预留（token）",output,{ output=it; config=config.copy(outputTokens=it.toIntOrNull() ?: 0) },keyboard=KeyboardType.Number)
                            Text("超出容量时分批处理已提供的历史，不会为省费只取最近几条。",style=MaterialTheme.typography.bodySmall)
                        } }
                        item(key="pricing") { BoxCard("人民币计价") {
                            Field("输入价（元 / 百万token）",config.inputRmbPerMillion,{ config=config.copy(inputRmbPerMillion=it); config=config.copy(pricingVerified=false) },keyboard=KeyboardType.Decimal)
                            Field("输出价（元 / 百万token）",config.outputRmbPerMillion,{ config=config.copy(outputRmbPerMillion=it,pricingVerified=false) },keyboard=KeyboardType.Decimal)
                            Field("缓存输入价（未知可留空）",config.cachedRmbPerMillion,{ config=config.copy(cachedRmbPerMillion=it,pricingVerified=false) },keyboard=KeyboardType.Decimal)
                            Field("价格来源、核对日期及汇率依据",config.pricingSource,{ config=config.copy(pricingSource=it,pricingVerified=false) },lines=2)
                            Check("我已核对人民币价格与模型容量",config.pricingVerified,{ config=config.copy(pricingVerified=it) })
                        } }
                        if(config.kind==ProviderKind.CHAT_COMPLETIONS) item(key="compatibility") { Disclosure("接口兼容选项",summary="JSON模式、流式与输出参数") {
                            Check("接口支持JSON输出模式",config.jsonMode,{ config=config.copy(jsonMode=it) })
                            Check("使用流式SSE（须返回usage）",config.stream,{ config=config.copy(stream=it) })
                            Pick("输出参数",listOf("max_tokens","max_completion_tokens"),config.maxTokensField,{ it },{ config=config.copy(maxTokensField=it) })
                        } }
                        item(key="save") { Button(onClick={ checkedSave() },modifier=Modifier.fillMaxWidth()) { Text("核验并保存接口") } }
                    }
                }
            }
        }
    }
}

@Composable internal fun FeeSettings(c: ProductController) {
    var confirm by remember { mutableStateOf(false) }
    var external by rememberSaveable { mutableStateOf("") }; var note by rememberSaveable { mutableStateOf("") }
    var externalChecked by rememberSaveable { mutableStateOf(false) }
    val month=Costing.month(System.currentTimeMillis())
    val months=(listOf(month)+c.charges.map { it.month }).distinct().sortedDescending()
    var selectedMonth by rememberSaveable { mutableStateOf(month) }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        BoxCard("$month · 本设备月度预算") {
            Text("¥${yuan(c.monthSpent)}",style=MaterialTheme.typography.headlineLarge)
            Text("已记录费用与未知用量预留 · 服务商账单为准",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress={ (c.monthSpent.toFloat()/c.monthLimit.coerceAtLeast(1)).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
            Text("已确认额度 ¥${yuan(c.monthLimit)}",style=MaterialTheme.typography.bodyMedium)
            Text("设备预算跨本机账户合计，以下调用明细仅显示当前主播。100元时提醒，保守预估将达到额度会先停止。",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick=c::refreshFees,enabled=!c.busy,modifier=Modifier.fillMaxWidth()) { Text("刷新费用记录") }
        }
        Disclosure("预算与账单补录",summary="增加额度需再次确认；其他软件的费用须人工核对。") {
            OutlinedButton(onClick={ confirm=true },enabled=!c.busy && !c.generating) { Text("确认本月再增加100元额度") }
            Hint("费用按真实usage乘已核验单价估算。上下文预估并非官方tokenizer，最终以服务商账单为准。未知费用继续占用预算。")
            Field("外部账单增量（元）",external,{ external=it; externalChecked=false },keyboard=KeyboardType.Decimal)
            Field("账单来源与核对日期",note,{ note=it; externalChecked=false })
            Check("我已核对这笔增量，没有重复计入",externalChecked,{ externalChecked=it })
            Button(onClick={ c.recordExternal(external,note); external=""; note=""; externalChecked=false },enabled=externalChecked && note.isNotBlank() && external.toBigDecimalOrNull()?.let { it>=java.math.BigDecimal.ZERO && it.scale()<=6 && it<=java.math.BigDecimal("100000000") }==true && !c.busy && !c.generating) { Text("计入本月提醒") }
        }
        BoxCard("当前主播的调用记录") {
            Pick("月份",months,selectedMonth,{ it },{ selectedMonth=it })
            val rows=c.charges.filter { it.month==selectedMonth }.sortedByDescending { it.at }
            if(rows.isEmpty()) Text("这个月尚无已记录调用。保存模型、整理聊天不会调用AI。",color=MaterialTheme.colorScheme.onSurfaceVariant)
            rows.forEach { charge ->
                Text(charge.component,style=MaterialTheme.typography.titleSmall)
                Text("${charge.model} · ¥${yuan(charge.chargedMicro)}",style=MaterialTheme.typography.bodyLarge)
                Text(when(charge.state) { "SETTLED" -> "已取得用量，费用按单价估算"; "UNCERTAIN" -> "用量未知，预留未释放"; "RESERVED" -> "执行中，已占用预留"; "USER_VERIFIED_EXTERNAL" -> "人工补录的账单增量"; "USER_RECONCILED" -> "已人工核对账单"; else -> charge.state },style=MaterialTheme.typography.bodySmall)
                Text(charge.usage?.let { "输入 ${it.input} / 输出 ${it.output} / 缓存 ${it.cachedInput}" } ?: "未取得服务商用量，不能按免费处理。",style=MaterialTheme.typography.bodySmall)
                Text(charge.pricingSource,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(charge.state in listOf("UNCERTAIN","RESERVED")) {
                    var bill by rememberSaveable(charge.id) { mutableStateOf("") }; var verified by rememberSaveable(charge.id) { mutableStateOf(false) }
                    Field("核对服务商账单后的实际费用（元）",bill,{ bill=it; verified=false },keyboard=KeyboardType.Decimal)
                    Check("我已核对该调用账单，确认这个金额",verified,{ verified=it })
                    OutlinedButton(onClick={ c.reconcile(charge,bill) },enabled=charge.state=="UNCERTAIN" && verified && bill.toBigDecimalOrNull()?.let { it>=java.math.BigDecimal.ZERO }==true && !c.busy && !c.generating) { Text(if(charge.state=="RESERVED") "仍在执行，请稍后核对" else "确认账单核对结果") }
                }
                HorizontalDivider()
            }
        }
    }
    if(confirm) AlertDialog(onDismissRequest={ confirm=false },title={ Text("增加本月额度100元？") },text={ Text("设备预算将增加100元。不会减少历史或自动关闭组件，未知费用仍需核对。") },confirmButton={ TextButton(onClick={ c.continueBudget(); confirm=false }) { Text("我确认继续") } },dismissButton={ TextButton(onClick={ confirm=false }) { Text("取消") } })
}
