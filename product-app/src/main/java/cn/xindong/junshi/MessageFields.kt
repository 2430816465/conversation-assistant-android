package cn.xindong.junshi

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import cn.xindong.core.Kind
import cn.xindong.core.Speaker
import java.time.OffsetDateTime
import java.time.ZoneId

internal fun speakerLabel(speaker: Speaker)=when(speaker) {
    Speaker.STREAMER -> "主播（我）"; Speaker.CONTACT -> "观众（对方）"; Speaker.UNKNOWN -> "未确定，需选择"
}
internal fun kindLabel(kind: Kind)=when(kind) {
    Kind.TEXT -> "文字"; Kind.EMOJI -> "文字表情"; Kind.IMAGE -> "图片"; Kind.VOICE -> "语音"
    Kind.VIDEO -> "视频"; Kind.STICKER -> "表情包"; Kind.REFERENCE -> "引用"; Kind.RECALL -> "撤回"; Kind.OTHER -> "其他"
}
@Composable internal fun MessageTime(value: String,change: (String)->Unit) {
    val context=LocalContext.current
    Field("发送时间（未知留空）",value,change)
    Actions {
        TextButton(onClick={
            val initial=runCatching { OffsetDateTime.parse(value) }.getOrElse { OffsetDateTime.now() }
            DatePickerDialog(context,{ _,year,month,day ->
                TimePickerDialog(context,{ _,hour,minute ->
                    val date=java.time.LocalDateTime.of(year,month+1,day,hour,minute).atZone(ZoneId.systemDefault()).toOffsetDateTime()
                    change(date.toString())
                },initial.hour,initial.minute,true).show()
            },initial.year,initial.monthValue-1,initial.dayOfMonth).show()
        }) { Text("选择日期和时间") }
        if(value.isNotBlank()) TextButton(onClick={ change("") }) { Text("时间未知，留空") }
    }
}
