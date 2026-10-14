package cn.xindong.junshi

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.xindong.core.*

private enum class Destination(val title: String,val glyph: Glyph) {
    ADVISOR("军师",Glyph.CHAT),HISTORY("聊天",Glyph.HISTORY),PROFILE("档案",Glyph.PEOPLE),ME("我的",Glyph.SETTINGS)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ProductScreen(c: ProductController,shared: String?,reviewRequest: Int,sharedDone: ()->Unit) {
    val activeAccount=c.account
    if(activeAccount==null) { LoginScreen(c,shared!=null); return }
    val context=LocalContext.current
    var destinationName by rememberSaveable(activeAccount.id) { mutableStateOf(Destination.ADVISOR.name) }
    val destination=Destination.valueOf(destinationName)
    var section by rememberSaveable(activeAccount.id) { mutableStateOf("") }
    var dialog by remember { mutableStateOf("") }
    var exportScope by remember { mutableStateOf<Scope?>(null) }
    val stateHolder=rememberSaveableStateHolder()
    // Capture the state key before lazy item callbacks can run after a scope change.
    val workspaceStateKey="${c.streamer?.id}/${c.current?.scope?.key}/${destination.name}/$section"
    val showStatus=c.busy || c.generating || !(c.status.startsWith("已选择主播 ") || c.status in listOf("已登录，请选择主播","账户已创建，请建立主播档案"))
    val modelDraft=c.settingsDraft
    val textFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(c::importTextFile) }
    val screenshots=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if(it.isNotEmpty()) c.importMedia(it,true) }
    val media=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if(it.isNotEmpty()) c.importMedia(it,false) }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val scope=exportScope; if(uri!=null && scope!=null) c.export(scope,uri); exportScope=null
    }
    fun navigate(next: Destination) { destinationName=next.name; section=""; if(next==Destination.ME) c.refreshFees() }
    LaunchedEffect(reviewRequest) { if(reviewRequest>0) { destinationName=Destination.ADVISOR.name; section="" } }
    BackHandler(section.isNotEmpty() || destination!=Destination.ADVISOR) {
        if(section.isNotEmpty()) section="" else destinationName=Destination.ADVISOR.name
    }
    val title=when(section) { "model" -> "模型与组件"; "fees" -> "费用与预算"; "read" -> "微信前台读取"; "help" -> "使用指南"; "data" -> "资料管理"; else -> when(destination) { Destination.ADVISOR -> "心动军师"; Destination.HISTORY -> "聊天资料"; Destination.PROFILE -> "观众档案"; Destination.ME -> "我的" } }
    Scaffold(containerColor=MaterialTheme.colorScheme.background,
        topBar={ TopAppBar(title={
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                if(section.isEmpty() && destination==Destination.ADVISOR) BrandMark(32)
                Text(title,style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        },
            navigationIcon={ if(section.isNotEmpty()) IconButton(onClick={ section="" }) { AppIcon(Glyph.BACK,"返回我的") } },
            actions={ IconButton(onClick={ dialog="logout" },enabled=!c.busy) { AppIcon(Glyph.LOCK,"退出并锁定账户") } },
            colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background)) },
        bottomBar={ Column {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp,modifier=Modifier.testTag("bottom-navigation")) {
            Destination.entries.forEach { item -> NavigationBarItem(selected=item==destination,onClick={ navigate(item) },
                icon={ AppIcon(item.glyph) },label={ Text(item.title,fontWeight=if(item==destination) FontWeight.Bold else FontWeight.Normal) },alwaysShowLabel=true,
                colors=NavigationBarItemDefaults.colors(selectedIconColor=MaterialTheme.colorScheme.primary,selectedTextColor=MaterialTheme.colorScheme.primary,
                    indicatorColor=MaterialTheme.colorScheme.primaryContainer,unselectedIconColor=MaterialTheme.colorScheme.onSurfaceVariant),
                modifier=Modifier.testTag("nav-${item.name}")) }
        } } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),contentAlignment=Alignment.TopCenter) {
            key(destination,section,c.current?.scope?.key,c.streamer?.id) {
                LazyColumn(Modifier.widthIn(max=840.dp).fillMaxWidth().testTag("workspace-scroll"),
                    verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(horizontal=24.dp,vertical=8.dp)) {
                    if(destination!=Destination.ME) item(key="context") {
                        ContextCard(c) { dialog="context" }
                    }
                    item(key="status") { if(showStatus) OperationStatus(c) }
                    item(key="content") {
                        stateHolder.SaveableStateProvider(workspaceStateKey) {
                            when {
                                destination==Destination.ME -> when(section) {
                                    "model" -> if(c.streamer==null) SetupCard(c,{ dialog="streamer" },{ dialog="contact" },{ dialog="context" }) else ModelSettings(c,modelDraft)
                                    "fees" -> if(c.streamer==null) SetupCard(c,{ dialog="streamer" },{ dialog="contact" },{ dialog="context" }) else FeeSettings(c)
                                    "read" -> ReadSettings(c)
                                    "help" -> HelpScreen()
                                    "data" -> DataScreen(c,{ dialog="export" },{ dialog="delete-contact" },{ dialog="delete-account" })
                                    else -> MyScreen(c,{ section=it },{ dialog="context" },{ dialog="password" },{ dialog="logout" })
                                }
                                c.current==null -> SetupCard(c,{ dialog="streamer" },{ dialog="contact" },{ dialog="context" })
                                destination==Destination.ADVISOR -> Advisor(c,{ navigate(Destination.HISTORY) },{ navigate(Destination.ME); section="model" })
                                destination==Destination.HISTORY -> HistoryScreen(c,{ textFile.launch(arrayOf("text/*","application/json")) },
                                    { screenshots.launch(arrayOf("image/*")) },{ media.launch(arrayOf("image/*","audio/*","video/*","application/octet-stream")) })
                                destination==Destination.PROFILE -> ProfileScreen(c)
                            }
                        }
                    }
                    item(key="footer") {
                        Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                            AppIcon(Glyph.LOCK,modifier=Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("回复由你决定，发送由你完成",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    if(dialog=="context") WorkspacePicker(c,{ dialog="" },{ dialog="streamer" },{ dialog="contact" })
    if(dialog in listOf("streamer","contact")) AskName(if(dialog=="streamer") "新建主播" else "新建观众",{ dialog="" }) {
        if(dialog=="streamer") c.createStreamer(it) else c.createContact(it)
    }
    if(dialog=="password") PasswordDialog(c) { dialog=""; c.clearAuthenticationError() }
    if(dialog=="logout") AlertDialog(onDismissRequest={ dialog="" },title={ Text("退出当前账户？") },
        text={ Text("已保存资料会保留，未保存的表单会清空。退出后读取和生成暂停，其他账户需用自己的密码登录。") },
        confirmButton={ TextButton(onClick={ dialog=""; c.lock() }) { Text("退出登录") } },dismissButton={ TextButton(onClick={ dialog="" }) { Text("取消") } })
    if(dialog in listOf("delete-contact","delete-account","export")) {
        val exporting=dialog=="export"; var typed by remember(dialog) { mutableStateOf("") }
        val expected=if(dialog=="delete-account") c.account?.name.orEmpty() else c.current?.contact?.name.orEmpty()
        AlertDialog(onDismissRequest={ dialog="" },title={ Text(if(exporting) "导出当前观众资料" else "永久删除${if(dialog=="delete-account") "账户" else "观众"}") },
            text={ Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(exporting) "ZIP包含当前观众的聊天、原始媒体和报告明文，不含模型密钥。请选择自己的保存位置；目前不支持ZIP一键恢复。" else "删除不可撤销，已导出的文件仍由你管理。请输入“$expected”确认。")
                if(!exporting) Field("输入名称确认",typed,{ typed=it })
            } },confirmButton={ TextButton(onClick={ when(dialog) {
                "delete-contact" -> c.deleteCurrent(); "delete-account" -> c.deleteAccount()
                "export" -> { exportScope=c.current?.scope; export.launch("xindong-${exportScope?.contactId}.zip") }
            }; dialog="" },enabled=exporting || typed==expected && expected.isNotBlank()) { Text(if(exporting) "选择位置并导出" else "永久删除") } },
            dismissButton={ TextButton(onClick={ dialog="" }) { Text("取消") } })
    }
    if(shared!=null && c.current!=null) AlertDialog(onDismissRequest=sharedDone,title={ Text("导入分享文字") },text={ Text("将材料导入“${c.current!!.contact.name}”，之后逐条校对归属和正文。") },
        confirmButton={ TextButton(onClick={ c.importText(shared); sharedDone(); navigate(Destination.HISTORY) }) { Text("导入待校对区") } },dismissButton={ TextButton(onClick=sharedDone) { Text("取消") } })
}

@Composable private fun ContextCard(c: ProductController,open: ()->Unit) {
    val account=c.account ?: return
    val conversation=c.current
    Surface(onClick=open,enabled=!c.busy,shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth().testTag("workspace-context")) {
        Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Avatar(conversation?.contact?.name ?: c.streamer?.name ?: account.name)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(conversation?.contact?.name ?: "选择主播与观众",style=MaterialTheme.typography.titleSmall)
                Text(if(conversation==null) "建立独立聊天档案后开始" else "主播 ${c.streamer?.name} · ${conversation.messages.size} 条聊天记忆",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppIcon(Glyph.CHEVRON,"切换主播与观众")
        }
    }
}
@Composable private fun OperationStatus(c: ProductController) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(c.busy || c.generating) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(c.status.isNotBlank()) Text(c.status,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.testTag("operation-status").padding(horizontal=4.dp))
    }
}
@Composable private fun SetupCard(c: ProductController,newStreamer: ()->Unit,newContact: ()->Unit,choose: ()->Unit) {
    BoxCard(if(c.streamer==null) "先建立主播档案" else "为一位观众建立独立记忆") {
        Text(if(c.streamer==null) "每位主播分别保存自己的口吻、观众资料和模型配置。" else "昵称相同也分别保存。先提供聊天，再生成属于这个人的回复。")
        Button(onClick=if(c.streamer==null) newStreamer else newContact,enabled=!c.busy,modifier=Modifier.fillMaxWidth()) { Text(if(c.streamer==null) "新建主播" else "新建观众") }
        if(c.streamers.isNotEmpty()) OutlinedButton(onClick=choose,modifier=Modifier.fillMaxWidth()) { Text("选择已有档案") }
    }
}
@Composable private fun WorkspacePicker(c: ProductController,close: ()->Unit,newStreamer: ()->Unit,newContact: ()->Unit) {
    var search by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=close,title={ Text("主播与观众") },text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("当前账户：${c.account?.name}",style=MaterialTheme.typography.bodySmall)
            Pick("主播",c.streamers,c.streamer,{ it.name },{ search=""; c.selectStreamer(it) })
            OutlinedButton(onClick=newStreamer,enabled=!c.busy) { AppIcon(Glyph.PLUS); Spacer(Modifier.width(8.dp)); Text("新建主播") }
            if(c.streamer!=null) {
                HorizontalDivider(); Field("查找观众",search,{ search=it })
                c.contacts.filter { it.contact.name.contains(search,ignoreCase=true) }.forEach { contact ->
                    MenuEntry(contact.contact.name,"${contact.messages.size} 条记录 · 独立ID ${contact.scope.contactId.take(6)}",Glyph.PEOPLE,
                        { c.selectContact(contact); close() },enabled=!c.busy)
                }
                if(c.contacts.isEmpty()) Text("还没有观众档案。",color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick=newContact,enabled=!c.busy) { AppIcon(Glyph.PLUS); Spacer(Modifier.width(8.dp)); Text("新建观众") }
            }
        }
    },confirmButton={ TextButton(onClick=close) { Text("完成") } })
}
@Composable private fun MyScreen(c: ProductController,open: (String)->Unit,workspace: ()->Unit,password: ()->Unit,logout: ()->Unit) {
    val account=c.account ?: return
    Column(verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Column(Modifier.padding(top=8.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                BrandMark(64)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text(account.name,style=MaterialTheme.typography.headlineSmall)
                    Text("本机独立账户 · ${c.streamers.size} 位主播",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Actions {
                FilledTonalButton(onClick=workspace,enabled=!c.busy) { Text("管理主播与观众") }
                TextButton(onClick={ c.clearAuthenticationError(); password() },enabled=!c.busy && !c.generating) { Text("修改密码") }
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Eyebrow("回复助手")
            Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface) {
                Column {
                    MenuEntry("模型与组件",c.settings.profiles.firstOrNull { it.id==c.settings.modelId }?.name ?: "连接模型，设置分析组件",Glyph.SPARK,{ open("model") })
                    HorizontalDivider(Modifier.padding(start=72.dp,end=16.dp),color=MaterialTheme.colorScheme.outlineVariant)
                    MenuEntry("微信前台读取",if(ReadBridge.autoRead) "本次已开启 · 可见文字需核对" else "授权读取与悬浮球",Glyph.CHAT,{ open("read") })
                }
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Eyebrow("账户与资料")
            Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface) {
                Column {
                    MenuEntry("费用与预算","本月 ¥${yuan(c.monthSpent)} · 查看用量",Glyph.WALLET,{ c.refreshFees(); open("fees") })
                    HorizontalDivider(Modifier.padding(start=72.dp,end=16.dp),color=MaterialTheme.colorScheme.outlineVariant)
                    MenuEntry("资料管理","导出、整理与删除资料",Glyph.IMPORT,{ open("data") })
                    HorizontalDivider(Modifier.padding(start=72.dp,end=16.dp),color=MaterialTheme.colorScheme.outlineVariant)
                    MenuEntry("使用指南","了解操作步骤与功能范围",Glyph.INFO,{ open("help") })
                }
            }
        }
        OutlinedButton(onClick=logout,modifier=Modifier.fillMaxWidth(),enabled=!c.busy) { AppIcon(Glyph.LOCK); Spacer(Modifier.width(8.dp)); Text("退出并锁定账户") }
        Text("心动军师 AI · ${BuildConfig.VERSION_NAME}\n预览版：手机稳定性与真实AI质量仍待完整验收。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun ReadSettings(c: ProductController) {
    val context=LocalContext.current
    var connected by remember { mutableStateOf(ChatReadService.current!=null) }
    LaunchedEffect(Unit) { while(true) { connected=ChatReadService.current!=null; kotlinx.coroutines.delay(1000) } }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        BoxCard("读取当前可见聊天") {
            Text(if(connected) "无障碍服务已连接" else "无障碍服务未连接",style=MaterialTheme.typography.titleSmall)
            Text("仅在你打开微信聊天时读取屏幕可见文字。无法获得微信全部历史或无通知的后台消息；OCR可能出错。")
            Button(onClick={ context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },modifier=Modifier.fillMaxWidth()) { Text("打开系统无障碍设置") }
            if(c.current!=null) Check("本次档案开启微信前台自动读取",ReadBridge.autoRead,c::setAutoRead)
            else Hint("请先选择主播与观众，再开启本次读取。")
            Text(ReadBridge.status,style=MaterialTheme.typography.bodySmall)
            Actions {
                OutlinedButton(onClick={ ChatReadService.current?.show() ?: context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("显示悬浮球") }
                TextButton(onClick={ ChatReadService.current?.hide() }) { Text("隐藏悬浮球") }
            }
        }
        BoxCard("建议的操作顺序") {
            Text("1. 选择对应的主播和观众。\n2. 授权后打开已授权的微信聊天。\n3. 用悬浮球回到军师页，核对最新对方消息。\n4. 确认后分析，选择候选并自行在微信发送。")
            Hint("切换档案、锁屏或撤权会暂停读取。首次历史通过复制文字或截图补充，缺失内容请在档案中标明。")
        }
    }
}
@Composable private fun DataScreen(c: ProductController,export: ()->Unit,deleteContact: ()->Unit,deleteAccount: ()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        BoxCard("当前观众资料") {
            Text(c.current?.contact?.name ?: "尚未选择观众",style=MaterialTheme.typography.titleSmall)
            Text("包含已保存聊天、原始媒体和分析报告；密钥不导出。当前导出用于查阅归档，暂不支持ZIP一键恢复。")
            Button(onClick=export,enabled=c.current!=null && !c.busy && !c.generating,modifier=Modifier.fillMaxWidth()) { Text("导出本观众完整资料") }
            TextButton(onClick=deleteContact,enabled=c.current!=null && !c.busy && !c.generating) { Text("删除此观众") }
        }
        BoxCard("账户数据") {
            Text("本机各账户分别登录，主播和观众资料按独立ID隔离。暂不提供跨设备同步。")
            Hint("删除账户会删除该账户全部主播、观众、聊天和模型配置。设备费用台账保留，防止未知用量被当成免费。")
            TextButton(onClick=deleteAccount,enabled=!c.busy && !c.generating,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)) { Text("删除当前本地账户及全部主播资料") }
        }
    }
}
@Composable private fun HelpScreen() {
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        SectionHeading("用真实材料，聊出你的语气","初次使用按以下顺序准备，日常从军师页开始。")
        BoxCard("01 选择主播与观众") { Text("顶部资料卡可以切换或创建档案。同名观众不会自动合并；发送前核对当前微信聊天。") }
        BoxCard("02 在聊天页补充历史") { Text("粘贴复制文字、导入UTF-8文字文件或批量截图。核对归属、方向、正文及顺序，再确认入库。图片、语音等原件可以附上完整描述或转写。") }
        BoxCard("03 在我的页面配置模型") { Text("填写自己的HTTPS接口、API Key、模型容量和人民币价格。双Skill默认开启，Jev可后续单独配置。阅读云端发送范围并决定是否授权，保存后生效。") }
        BoxCard("04 获取回复并手动发送") { Text("在军师页逐条填入或核对最新对方消息，确认后分析。也可以基于现有历史主动生成。选择、修改并复制候选，自己在微信发送；实际发送后可记录最终文字供口吻学习。") }
        BoxCard("功能边界与测试状态") {
            Text("读取只覆盖当前可见屏幕；不能直接取得全部微信历史。导入材料不等于已取得所有记录，缺口会随分析保留。")
            Text("没有真实已发送的主播样本时，无法验证口吻模仿。当前100组质量及对照测试已停止，不能宣称已达到95/100。")
            Text("本地整理可离线使用；真实AI调用由你配置的服务商计费，失败或格式修复也可能产生费用。",style=MaterialTheme.typography.bodyMedium)
        }
    }
}
