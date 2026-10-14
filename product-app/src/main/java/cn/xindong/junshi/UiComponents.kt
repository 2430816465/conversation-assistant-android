package cn.xindong.junshi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp

/** Local line vectors: one stroke family, no remote assets or icon dependencies. */
internal enum class Glyph(private val data: String) {
    CHAT("M4,4 H20 V16 H10 L5,20 V16 H4 Z M8,8 H16 M8,12 H13"),
    HISTORY("M4,8 A9,9 0,1 1,3,14 M4,3 V8 H9 M12,7 V12 L15,14"),
    PEOPLE("M9,11 A4,4 0,1 0,9,3 A4,4 0,1 0,9,11 M2,21 V18 Q2,14 9,14 Q16,14 16,18 V21 M17,4 Q23,5 19,11 M19,14 Q23,15 22,21"),
    SETTINGS("M12,8 A4,4 0,1 0,12,16 A4,4 0,1 0,12,8 M10,2 H14 L15,5 L18,6 L21,6 L23,10 L21,12 L22,15 L19,18 L16,18 L14,22 H10 L9,19 L6,18 H3 L1,14 L3,12 L2,9 L5,6 H8 Z"),
    CHEVRON("M9,5 L16,12 L9,19"), BACK("M15,5 L8,12 L15,19"),
    PLUS("M12,5 V19 M5,12 H19"), CHECK("M4,12 L9,17 L20,6"),
    LOCK("M7,10 V7 A5,5 0,0 1,17,7 V10 M5,10 H19 V22 H5 Z M12,15 V18"),
    WALLET("M20,6 H4 V3 H18 V6 M4,6 Q2,6 2,9 V19 Q2,21 4,21 H21 V8 H4 M21,12 H16 V17 H21"),
    IMPORT("M12,2 V14 M7,9 L12,14 L17,9 M4,15 V21 H20 V15"),
    SPARK("M12,2 L15,9 L22,12 L15,15 L12,22 L9,15 L2,12 L9,9 Z"),
    KEY("M8,3 A5,5 0,1 0,8,13 A5,5 0,1 0,8,3 M12,11 L21,20 M17,16 L19,14 M20,19 L22,17"),
    HEART("M12,21 L3,12 C-2,5 6,-1 12,6 C18,-1 26,5 21,12 Z"),
    COPY("M9,8 H21 V22 H9 Z M15,8 V2 H3 V16 H9"),
    ARROW("M4,12 H20 M14,6 L20,12 L14,18"),
    INFO("M12,2 A10,10 0,1 0,12,22 A10,10 0,1 0,12,2 M12,10 V17 M12,6 V6.5");
    val vector: ImageVector by lazy {
        ImageVector.Builder(name=name,defaultWidth=24.dp,defaultHeight=24.dp,viewportWidth=24f,viewportHeight=24f)
            .addPath(pathData=PathParser().parsePathString(data).toNodes(),fill=null,stroke=SolidColor(Color.Black),strokeLineWidth=1.7f,strokeLineCap=StrokeCap.Round,strokeLineJoin=StrokeJoin.Round).build()
    }
}
@Composable internal fun AppIcon(glyph: Glyph, description: String?=null, modifier: Modifier=Modifier) {
    Icon(glyph.vector,description,modifier.size(24.dp))
}
@Composable internal fun BrandMark(size: Int=56) {
    Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.size(size.dp)) {
        Box(contentAlignment=Alignment.Center) {
            Icon(Glyph.CHAT.vector,null,Modifier.size((size*0.62f).dp),tint=MaterialTheme.colorScheme.onPrimary)
            Icon(Glyph.HEART.vector,null,Modifier.size((size*0.22f).dp).align(Alignment.TopEnd).padding(end=2.dp,top=2.dp),tint=MaterialTheme.colorScheme.onPrimary)
        }
    }
}
/** Decorative chat illustration, drawn locally and excluded from accessibility semantics. */
@Composable internal fun ConversationArt(modifier: Modifier=Modifier) {
    val colors=MaterialTheme.colorScheme
    Canvas(modifier) {
        val scale=size.minDimension/120f
        fun point(x: Float,y: Float)=Offset(x*scale,y*scale)
        drawCircle(colors.primaryContainer,radius=52*scale,center=point(64f,60f))
        drawRoundRect(colors.secondaryContainer,point(6f,19f),Size(80*scale,50*scale),CornerRadius(17*scale))
        drawRoundRect(colors.surface,point(31f,53f),Size(83*scale,54*scale),CornerRadius(17*scale))
        val tail=Path().apply { moveTo(88*scale,98*scale); lineTo(101*scale,113*scale); lineTo(102*scale,94*scale); close() }
        drawPath(tail,colors.surface)
        drawLine(colors.onSecondaryContainer,point(24f,36f),point(65f,36f),3*scale,StrokeCap.Round)
        drawLine(colors.onSecondaryContainer,point(24f,48f),point(50f,48f),3*scale,StrokeCap.Round)
        val heart=Path().apply {
            moveTo(73*scale,91*scale); cubicTo(62*scale,84*scale,55*scale,75*scale,64*scale,70*scale)
            cubicTo(68*scale,68*scale,72*scale,71*scale,73*scale,74*scale)
            cubicTo(76*scale,68*scale,84*scale,68*scale,87*scale,74*scale)
            cubicTo(91*scale,81*scale,81*scale,87*scale,73*scale,91*scale); close()
        }
        drawPath(heart,colors.primary)
        drawCircle(colors.primary,radius=3*scale,center=point(109f,23f))
        drawCircle(colors.outlineVariant,radius=4*scale,center=point(13f,91f))
    }
}
@Composable internal fun Eyebrow(text: String) {
    Text(text,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
}
@Composable internal fun IconBadge(glyph: Glyph,modifier: Modifier=Modifier) {
    Surface(modifier.size(44.dp),shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment=Alignment.Center) { Icon(glyph.vector,null,Modifier.size(22.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer) }
    }
}
@Composable internal fun Avatar(name: String, modifier: Modifier=Modifier) {
    Surface(modifier.size(44.dp),shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment=Alignment.Center) { Text(name.take(1).ifBlank { "人" },style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onPrimaryContainer) }
    }
}
@Composable internal fun Field(label: String,value: String,change: (String)->Unit,secret: Boolean=false,lines: Int=1,
    error: String?=null,keyboard: KeyboardType=KeyboardType.Text,modifier: Modifier=Modifier) {
    var reveal by remember { mutableStateOf(false) }
    OutlinedTextField(value,change,label={ Text(label,style=MaterialTheme.typography.bodyMedium) },modifier=modifier.fillMaxWidth(),minLines=lines,
        maxLines=if(secret) 1 else maxOf(lines,8),singleLine=secret,isError=error!=null,
        supportingText=error?.let { { Text(it) } },
        trailingIcon=if(secret) ({ TextButton(onClick={ reveal=!reveal }) { Text(if(reveal) "隐藏" else "显示") } }) else null,
        visualTransformation=if(secret && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions=KeyboardOptions(keyboardType=if(secret) KeyboardType.Password else keyboard,autoCorrectEnabled=if(secret) false else null),
        colors=OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor=MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor=MaterialTheme.colorScheme.surface,
            unfocusedBorderColor=MaterialTheme.colorScheme.outline,
            focusedBorderColor=MaterialTheme.colorScheme.primary),
        shape=MaterialTheme.shapes.medium)
}
@Composable internal fun Check(label: String,value: Boolean,change: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value,role=Role.Checkbox,onValueChange=change).padding(vertical=4.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Checkbox(value,onCheckedChange=null); Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun Actions(content: @Composable FlowRowScope.()->Unit) {
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp),content=content)
}
@Composable internal fun BoxCard(title: String,content: @Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.large,
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium); content()
        }
    }
}
@Composable internal fun Disclosure(title: String,initiallyOpen: Boolean=false,summary: String?=null,content: @Composable ColumnScope.()->Unit) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min=72.dp)
                .semantics { stateDescription=if(open) "已展开" else "已收起" }
                .clickable(role=Role.Button,onClickLabel=if(open) "收起$title" else "展开$title") { open=!open }.padding(20.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(title,style=MaterialTheme.typography.titleSmall)
                    summary?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                AppIcon(Glyph.CHEVRON,modifier=Modifier.rotate(if(open) 270f else 90f))
            }
            if(open) Column(Modifier.padding(horizontal=20.dp).padding(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
        }
    }
}
@Composable internal fun Hint(text: String,error: Boolean=false) {
    Surface(shape=MaterialTheme.shapes.small,color=if(error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(Glyph.INFO.vector,null,Modifier.size(20.dp),tint=if(error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=if(error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable internal fun SectionHeading(title: String,subtitle: String) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(title,style=MaterialTheme.typography.headlineMedium)
        Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable internal fun <T> Pick(label: String,values: List<T>,selected: T?,name: (T)->String,choose: (T)->Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick={ open=true },modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,
            contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),enabled=values.isNotEmpty(),
            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
            Text(label+"："+(selected?.let(name) ?: if(values.isEmpty()) "暂无可选项" else "请选择"),Modifier.weight(1f))
            AppIcon(Glyph.CHEVRON)
        }
        DropdownMenu(open,{ open=false },modifier=Modifier.heightIn(max=360.dp)) {
            values.forEach { value -> DropdownMenuItem(text={ Text(name(value)) },onClick={ choose(value); open=false }) }
        }
    }
}
@Composable internal fun MenuEntry(title: String,subtitle: String,glyph: Glyph,click: ()->Unit,enabled: Boolean=true) {
    Surface(onClick=click,enabled=enabled,shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface,
        modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            IconBadge(glyph)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,style=MaterialTheme.typography.titleSmall)
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppIcon(Glyph.CHEVRON)
        }
    }
}
