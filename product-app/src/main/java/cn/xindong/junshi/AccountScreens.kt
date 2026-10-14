package cn.xindong.junshi

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.xindong.core.Account

@Composable internal fun LoginScreen(c: ProductController,hasSharedText: Boolean) {
    var register by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(c.busy,c.accounts.size) {
        if(!c.busy && !initialized) { register=c.accounts.isEmpty(); initialized=true }
    }
    var name by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    BackHandler(register && c.accounts.isNotEmpty()) { register=false; localError=null; c.clearAuthenticationError() }
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(),contentAlignment=Alignment.Center) {
        Column(Modifier.widthIn(max=480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=28.dp,vertical=28.dp),
            verticalArrangement=Arrangement.spacedBy(28.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                BrandMark(36)
                Text("心动军师",style=MaterialTheme.typography.titleLarge)
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("聊得从容，\n回复像你。",style=MaterialTheme.typography.headlineLarge)
                    Text("你的语气，你来决定。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ConversationArt(Modifier.size(100.dp))
            }
            Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(if(register) "创建你的本机账号" else "欢迎回来",style=MaterialTheme.typography.titleLarge)
                    Text(if(register) "为你的聊天建立一个独立空间。" else "登录你的独立聊天空间。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(!register && c.accounts.isNotEmpty()) Pick("选择已有账号",c.accounts,c.accounts.firstOrNull { it.id==selectedId },
                    { a -> if(c.accounts.count { it.name.trim().equals(a.name.trim(),true) }>1) "${a.name} · ${a.id.take(6)}" else a.name }) {
                    name=it.name; selectedId=it.id; localError=null; c.clearAuthenticationError()
                }
                Field("账号名称",name,{ name=it; selectedId=null; localError=null; c.clearAuthenticationError() },modifier=Modifier.testTag("account-name"))
                Field("密码",password,{ password=it; localError=null; c.clearAuthenticationError() },secret=true,modifier=Modifier.testTag("account-password"))
                if(register) Field("再次输入密码",confirmation,{ confirmation=it; localError=null },secret=true,modifier=Modifier.testTag("account-confirmation"))
                val error=localError ?: c.authenticationError
                error?.let { Hint(it,error=true) }
                if(c.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button(onClick={
                    localError=when {
                        name.trim().isEmpty() -> "请填写账号名称"
                        register && name.trim().length>40 -> "账号名称最多40个字符"
                        password.length<6 -> "密码至少6位"
                        register && confirmation!=password -> "两次输入的密码不一致"
                        else -> null
                    }
                    if(localError==null) {
                        if(register) c.createAccount(name,password)
                        else {
                            val matches=c.accounts.filter { it.name.trim().equals(name.trim(),ignoreCase=true) }
                            val target=matches.firstOrNull { it.id==selectedId } ?: matches.singleOrNull()
                            if(target==null) localError=if(matches.isEmpty()) "这个账号尚未在本机创建，请检查名称或注册" else "有同名旧账号，请从上方列表选择具体账号"
                            else c.unlock(target,password)
                        }
                    }
                },enabled=!c.busy,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp).testTag("account-submit")) {
                    Text(if(c.busy) "正在处理…" else if(register) "创建并登录" else "登录")
                }
                TextButton(onClick={ register=!register; password=""; confirmation=""; localError=null; c.clearAuthenticationError() },enabled=!c.busy,modifier=Modifier.fillMaxWidth()) {
                    Text(if(register) "返回登录" else "注册本机账号")
                }
            }
            if(hasSharedText) Hint("有一份分享文字等待导入，登录并选择观众后可继续。")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                AppIcon(Glyph.LOCK,modifier=Modifier.size(16.dp))
                Text("资料加密保存在本机，各账户分别登录。暂不支持跨设备同步或密码找回；卸载会删除资料。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("预览版 ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable internal fun AskName(title: String,close: ()->Unit,submit: (String)->Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=close,title={ Text(title) },text={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(title.contains("观众")) "即使昵称相同，也会建立独立档案。" else "每位主播分别保存观众、口吻和模型设置。",style=MaterialTheme.typography.bodyMedium)
            Field("名称",name,{ name=it })
        }
    },confirmButton={ TextButton(onClick={ submit(name.trim()); close() },enabled=name.isNotBlank() && name.trim().length<=80) { Text("创建") } },dismissButton={ TextButton(onClick=close) { Text("取消") } })
}

@Composable internal fun PasswordDialog(c: ProductController,close: ()->Unit) {
    var old by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(c.passwordChanged) { if(c.passwordChanged) close() }
    AlertDialog(onDismissRequest={ if(!c.busy) close() },title={ Text("修改账户密码") },text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("${c.account?.name} · 修改后资料会保留。",style=MaterialTheme.typography.bodyMedium)
            Field("当前密码",old,{ old=it; error=null; c.clearAuthenticationError() },secret=true)
            Field("新密码（至少6位）",next,{ next=it; error=null },secret=true)
            Field("确认新密码",confirmation,{ confirmation=it; error=null },secret=true)
            (error ?: c.authenticationError)?.let { Hint(it,error=true) }
            if(c.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    },confirmButton={ TextButton(onClick={
        error=when { old.length<6 -> "请填写当前密码"; next.length<6 -> "新密码至少6位"; next!=confirmation -> "两次新密码不一致"; else -> null }
        if(error==null) c.changePassword(old,next)
    },enabled=!c.busy) { Text("保存新密码") } },dismissButton={ TextButton(onClick=close,enabled=!c.busy) { Text("取消") } })
}
