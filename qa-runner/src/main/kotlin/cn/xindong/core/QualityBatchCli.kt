package cn.xindong.core

import org.json.JSONObject
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Real, finite, synthetic-only A/B experiment. Failure is an outcome, never replaced by a fixture. */
object QualityBatchCli {
    private const val PROTOCOL="SYNTHETIC_PAIRED_DUAL_SKILL_V3"
    @JvmStatic fun main(args: Array<String>) {
        try { run(args) } catch(e: Exception) {
            System.err.println("对照实验停止："+AnalysisEngine.safeError(e)); kotlin.system.exitProcess(2)
        }
    }
    private fun write(path: Path, value: JSONObject) {
        Files.createDirectories(path.parent)
        val temp=path.resolveSibling(path.fileName.toString()+".tmp")
        Files.writeString(temp,value.toString(2),Charsets.UTF_8)
        try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING) }
        catch(_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING) }
    }
    private fun json(c: Charge)=JSONObject().put("id",c.id).put("owner",c.owner).put("month",c.month).put("component",c.component)
        .put("model",c.model).put("reserved",c.reservedMicro).put("charged",c.chargedMicro).put("state",c.state)
        .put("at",c.at).put("pricingSource",c.pricingSource).put("usage",c.usage?.let {
            JSONObject().put("input",it.input).put("output",it.output).put("cached",it.cachedInput) })
    private fun parseCharge(c: JSONObject)=Charge(c.getString("id"),c.getString("owner"),c.getString("month"),
        c.getString("component"),c.getString("model"),c.getLong("reserved"),c.getLong("charged"),
        c.optJSONObject("usage")?.let { Usage(it.getLong("input"),it.getLong("output"),it.getLong("cached")) },
        c.getString("state"),c.getLong("at"),c.optString("pricingSource"))
    private fun run(args: Array<String>) {
        val parts=args.toList().chunked(2)
        require(parts.all { it.size==2 } && parts.map { it[0] }.distinct().size==parts.size)
        val opts=parts.associate { it[0] to it[1] }
        require(opts.keys.all { it in setOf("--config","--cases","--out","--action","--resume") })
        val action=opts["--action"] ?: "check"; require(action in setOf("check","run"))
        val configPath=Path.of(opts["--config"] ?: error("缺少配置路径")).toAbsolutePath().normalize()
        require(configPath.any { it.toString()=="private" })
        val config=JSONObject(Files.readString(configPath,Charsets.UTF_8))
        val initial=AiSettings.parse(config.getJSONObject("settings"))
        val key=System.getenv("XINDONG_QA_KEY")
        val settings=if(key.isNullOrBlank()) initial else initial.copy(profiles=initial.profiles.map {
            if(it.id==initial.modelId) it.copy(apiKey=key) else it })
        require(!settings.jevEnabled && settings.goutouEnabled && settings.lianaiEnabled)
        val model=settings.profile(settings.modelId,ProviderKind.CHAT_COMPLETIONS)
        require(settings.cloudConsent && settings.componentNoticeAccepted && config.optBoolean("syntheticPairedTestAuthorized")) {
            "须有本批虚构材料真实双Skill对照测试授权" }
        val cap=config.getLong("batchCapMicroRmb"); require(cap in 1..60_000_000L)
        val prior=config.getLong("knownPreviousEstimateMicroRmb"); require(prior in 0 until 100_000_000L)
        val workers=config.optInt("workers",4); require(workers in 1..4)
        val casesPath=Path.of(opts["--cases"] ?: error("缺少测试集合")).toAbsolutePath().normalize()
        val cases=Files.readAllLines(casesPath,Charsets.UTF_8).filter { it.isNotBlank() }.map(::JSONObject)
        require(cases.size==100 && cases.map { it.getString("id") }.distinct().size==100)
        cases.forEach { c ->
            require(c.getString("provenance")=="SYNTHETIC_NOT_REAL_WECHAT" && c.getString("id").matches(Regex("[A-Za-z0-9_-]+")))
            val history=HistoryJson.parse(c.getJSONObject("conversation").toString())
            require(history.messages.last().speaker==Speaker.CONTACT)
        }
        val skills=SkillLibrary.bundled()
        val publicSettings=settings.json().also { it.objects("profiles").forEach { p -> p.remove("apiKey") } }
        val settingsHash=sha256(publicSettings.toString().toByteArray(Charsets.UTF_8))
        val casesHash=sha256(Files.readAllBytes(casesPath))
        val skillHash=sha256((skills.goutou+"\n"+skills.lianai).toByteArray(Charsets.UTF_8))
        val engineHash=sha256(listOf(AnalysisEngine::class.java,AnalysisFormat::class.java,StructuredChat::class.java,
            SkillOutput::class.java,Ledger::class.java,HttpGateway::class.java,QualityBatchCli::class.java).joinToString("\n") { type ->
            type.getResourceAsStream("/${type.name.replace('.','/')}.class")!!.use { sha256(it.readBytes()) }
        }.toByteArray(Charsets.UTF_8))
        val check=JSONObject().put("runner_protocol",PROTOCOL).put("output_protocol",StructuredChat.PROTOCOL)
            .put("architecture","DUAL_SKILL_V1").put("expected_components",arr(listOf("狗头军师","恋爱大师")))
            .put("settings",publicSettings).put("settings_sha256",settingsHash).put("cases_sha256",casesHash)
            .put("skill_sha256",skillHash).put("engine_sha256",engineHash).put("requested",100).put("workers",workers).put("batch_cap_micro_rmb",cap)
            .put("known_previous_estimate_micro_rmb",prior).put("provider_bill_verified",false)
            .put("billing_basis","KNOWN_LOCAL_TOKEN_ESTIMATES_NOT_ENTIRE_PROVIDER_BILL")
            .put("human_graded",0).put("quality_pass",JSONObject.NULL)
        if(action=="check") { println(check.toString(2)); println("配置与100组材料校验完成；未调用接口。"); return }
        val out=Path.of(opts["--out"] ?: error("缺少私有输出路径")).toAbsolutePath().normalize()
        require(out.any { it.toString()=="private" })
        Files.createDirectories(out.parent)
        val exists=Files.exists(out)
        require(!exists || opts["--resume"]=="true") { "已存在输出目录；禁止无意重复付费，恢复须显式resume" }
        Files.createDirectories(out)
        FileChannel.open(out.resolve("batch.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE).use { channel ->
            (channel.tryLock() ?: error("同一实验已在运行")).use {
                val planPath=out.resolve("run-plan.json")
                val plan=if(exists) JSONObject(Files.readString(planPath,Charsets.UTF_8)).also {
                    for(field in listOf("runner_protocol","output_protocol","settings_sha256","cases_sha256","skill_sha256","engine_sha256"))
                        require(it.getString(field)==check.getString(field)) { "输入或实验协议变化，须新建目录" }
                    require(it.getLong("batch_cap_micro_rmb")==cap && it.getLong("known_previous_estimate_micro_rmb")==prior)
                } else check.put("started_at",System.currentTimeMillis()).put("plans",arr(cases.map { c ->
                    val plain=if(SecureRandom().nextBoolean()) "A" else "B"
                    JSONObject().put("id",c.getString("id")).put("ordinary",plain).put("full",if(plain=="A") "B" else "A") }))
                write(planPath,plan)
                execute(out,cases,plan,settings,skills,cap,prior,workers)
            }
        }
    }
    private fun execute(out: Path,cases: List<JSONObject>,plan: JSONObject,settings: AiSettings,skills: SkillLibrary,
        cap: Long,prior: Long,workers: Int) {
        val month=Costing.month(System.currentTimeMillis()); val ledgerPath=out.resolve("quality-ledger.json")
        val saved=if(Files.exists(ledgerPath)) JSONObject(Files.readString(ledgerPath,Charsets.UTF_8)) else null
        val starting=saved?.objects("rows")?.map(::parseCharge) ?: listOf(Charge(id="known-previous-estimate",owner="external-local-estimates",
            month=month,component="已知本地历史估算，未核对服务商全部账单",model="external",reservedMicro=0,chargedMicro=prior,state="KNOWN_EXTERNAL_ESTIMATE"))
        val ledger=Ledger(starting,mapOf(month to minOf(100_000_000L,Math.addExact(prior,cap))),workers) { rows,limits ->
            write(ledgerPath,JSONObject().put("rows",arr(rows.map(::json))).put("limits",JSONObject(limits)).put("provider_bill_verified",false)) }
        val stop=AtomicBoolean(false); val cancel=CancelToken(); val writeLock=Any()
        val shutdown=Thread { stop.set(true); cancel.cancel() }; Runtime.getRuntime().addShutdownHook(shutdown)
        val plans=plan.objects("plans").associateBy { it.getString("id") }
        val mappings=if(Files.exists(out.resolve("blind-mapping.json")))
            JSONObject(Files.readString(out.resolve("blind-mapping.json"),Charsets.UTF_8)).objects("rows").associateBy { it.getString("id") }.toMutableMap()
            else mutableMapOf<String,JSONObject>()
        fun summary(state: String,error: String="")=synchronized(writeLock) {
            val rows=mappings.values.sortedBy { it.getString("id") }
            write(out.resolve("blind-mapping.json"),JSONObject().put("rows",arr(rows)))
            val charges=ledger.entries().filter { it.id!="known-previous-estimate" }
            write(out.resolve("run-summary.json"),JSONObject().put("state",state).put("cases",rows.size).put("requested",100)
                .put("architecture","DUAL_SKILL_V1").put("cases_sha256",plan.getString("cases_sha256"))
                .put("settings_sha256",plan.getString("settings_sha256")).put("real_interface_calls",charges.isNotEmpty())
                .put("call_count",charges.size).put("usage_estimate_micro_rmb",ledger.spent(month)-prior)
                .put("known_total_estimate_micro_rmb",ledger.spent(month)).put("provider_bill_verified",false)
                .put("failed_variants",rows.sumOf { it.getInt("failed_variants") }).put("quality_pass",JSONObject.NULL)
                .put("human_graded",0).put("error",error).put("updated_at",System.currentTimeMillis()))
        }
        fun fatal(e: Exception): Boolean = e is ApiFailure || e is java.io.IOException || e is InterruptedException ||
            ledger.entries().any { it.state=="UNCERTAIN" } || e.message?.contains("额度")==true
        val executor=Executors.newFixedThreadPool(workers)
        summary("IN_PROGRESS")
        try {
            val futures=cases.map { case -> executor.submit {
                val id=case.getString("id")
                if(stop.get() || synchronized(writeLock) { mappings.containsKey(id) }) return@submit
                val conversation=HistoryJson.parse(case.getJSONObject("conversation").toString())
                val tail=conversation.messages.filter { it.status==ReviewStatus.CONFIRMED }.sortedBy { it.order }.takeLastWhile { it.speaker==Speaker.CONTACT }
                val target=ReplyTarget(conversation.scope,tail.map { it.id },tail.map { it.revision }).validate(conversation)
                val host=Streamer(conversation.scope.streamerId,conversation.scope.accountId,"虚构测试主播")
                val inputHash=sha256(case.toString().toByteArray(Charsets.UTF_8)); val assignment=plans.getValue(id)
                val model=settings.profile(settings.modelId,ProviderKind.CHAT_COMPLETIONS)
                val activeCharge=ThreadLocal<String>()
                var currentCharges=mutableListOf<String>()
                val budget=object: Budget {
                    override fun reserve(owner: String,component: String,model: ModelConfig,upperInput: Int): String {
                        if(stop.get()) throw InterruptedException("实验停止")
                        return ledger.reserve("$id/$owner",component,model,upperInput).also { activeCharge.set(it); currentCharges.add(it) }
                    }
                    override fun settle(id: String,usage: Usage?,model: ModelConfig,rejectedBeforeInference: Boolean) =
                        ledger.settle(id,usage,model,rejectedBeforeInference)
                }
                val gateway=object: Gateway {
                    private val real=HttpGateway()
                    override fun chat(config: ModelConfig,system: String,user: String,cancel: CancelToken,progress: (String)->Unit): ModelReply {
                        val reply=real.chat(config,system,user,cancel,progress)
                        val charge=ledger.entries().single { it.id==activeCharge.get() }
                        write(out.resolve("$id-response-${charge.id}.json"),JSONObject().put("case",id).put("charge_id",charge.id)
                            .put("component",charge.component).put("model",reply.model).put("text",reply.text)
                            .put("text_sha256",sha256(reply.text.toByteArray(Charsets.UTF_8))))
                        return reply
                    }
                    override fun jev(config: ModelConfig,state: JSONObject,cancel: CancelToken): ModelReply = error("本批不调用Jev")
                }
                fun generate(scheme: String): JSONObject {
                    val path=out.resolve("$id-$scheme-audit.json")
                    val hashField=if(scheme=="full") "case_input_sha256" else "input_sha256"
                    if(Files.exists(path)) return JSONObject(Files.readString(path,Charsets.UTF_8)).also { require(it.getString(hashField)==inputHash) }
                    val components=mutableListOf<JSONObject>()
                    currentCharges=mutableListOf()
                    println("$id $scheme 开始；不输出正文或密钥")
                    val audit=try {
                        val success=if(scheme=="full") AnalysisEngine(gateway,budget,skills).run(conversation,host,listOf(conversation),settings,
                            case.getString("query"),cancel,progress={ println("$id $it") },component={ c ->
                                components.add(JSONObject().put("name",c.name).put("state",c.state).put("diagnostic",c.diagnostic ?: JSONObject.NULL))
                            },replyTarget=target).json()
                        else {
                            val user=JSONObject().put("profile",Contexts.metadata(conversation)).put("host_style",StyleLearning.profile(host,listOf(conversation),conversation.scope))
                                .put("host_style_notes",settings.styleNotes).put("request",case.getString("query"))
                                .put("history",arr(Contexts.history(conversation))).put("reply_target",target.json(conversation))
                            val reply=StructuredChat.run(ChargedGateway(gateway,budget,"quality-ordinary"),"普通模型对照",model,
                                AnalysisEngine.common+"\n"+AnalysisFormat.contract,user.toString(),cancel,progress={ println("$id $it") }) {
                                AnalysisFormat.parse(it,conversation.messages.map { m -> m.id }.toSet(),AnalysisFormat.explicitPause(conversation)) }
                            JSONObject().put("result",reply.value.raw).put("model",reply.reply.model)
                                .put("reply_target",target.json(conversation)).put("format_repairs",arr(reply.repairs))
                        }
                        success.put("generation_status","SUCCEEDED")
                    } catch(e: Exception) {
                        if(fatal(e)) { stop.set(true); throw e }
                        println("$id $scheme 校验失败；保留失败计入100组，不替换样本")
                        JSONObject().put("generation_status","FAILED").put("error",AnalysisEngine.safeError(e))
                            .put("components",arr(components)).put("failure_class",e.javaClass.simpleName)
                            .put("diagnostic",(e as? SkillOutputFailure)?.diagnostic ?: JSONObject.NULL)
                    }
                    audit.put(hashField,inputHash).put("model_config_sha256",plan.getString("settings_sha256"))
                        .put("call_ids",arr(currentCharges)).put("real_interface_calls",currentCharges.isNotEmpty())
                    write(path,audit); return audit
                }
                val firstScheme=if(assignment.getString("ordinary")=="A") "ordinary" else "full"
                val first=generate(firstScheme); if(stop.get()) return@submit
                val second=generate(if(firstScheme=="ordinary") "full" else "ordinary")
                fun blind(audit: JSONObject): JSONObject {
                    if(audit.getString("generation_status")=="FAILED") return JSONObject().put("generation_status","FAILED")
                        .put("generation_error","本方案未产生通过严格校验的回复，按失败计入分母")
                        .put("no_reply",false).put("no_reply_reason","").put("candidates",arr(emptyList<Any>()))
                    val result=audit.getJSONObject("result")
                    return JSONObject().put("generation_status","SUCCEEDED").put("no_reply",result.getBoolean("no_reply"))
                        .put("no_reply_reason",result.getString("no_reply_reason")).put("candidates",arr(result.objects("candidates").map { c ->
                            JSONObject().put("style",c.getString("style")).put("segments",c.getJSONArray("segments")) }))
                }
                val pair=JSONObject().put("id",id).put("provenance",case.getString("provenance")).put("input_sha256",inputHash)
                    .put("conversation",case.getJSONObject("conversation")).put("query",case.getString("query"))
                    .put("rubric",case.getJSONObject("rubric")).put("A",blind(first)).put("B",blind(second))
                val grading=out.resolve("grading/$id.json"); write(grading,pair)
                synchronized(writeLock) {
                    mappings[id]=JSONObject(assignment.toString()).put("input_sha256",inputHash)
                        .put("grading_sha256",sha256(Files.readAllBytes(grading)))
                        .put("failed_variants",listOf(first,second).count { it.getString("generation_status")=="FAILED" })
                }
                summary("IN_PROGRESS"); println("$id 两方案已保存；已完成组数=${synchronized(writeLock) { mappings.size }}/100")
            } }
            var failure: Exception?=null
            futures.forEach { future -> try { future.get() } catch(e: Exception) {
                stop.set(true); if(failure==null) failure=(e.cause as? Exception) ?: e
            } }
            failure?.let { summary("INCOMPLETE",AnalysisEngine.safeError(it)); throw it }
            require(mappings.size==100) { "实验未完成100组" }
            summary("GENERATED_AWAITING_REVIEW")
            println("100组真实对照生成结束；包含校验失败，等待独立审阅。人工验收尚未完成。")
        } finally { executor.shutdownNow(); Runtime.getRuntime().removeShutdownHook(shutdown) }
    }
}
