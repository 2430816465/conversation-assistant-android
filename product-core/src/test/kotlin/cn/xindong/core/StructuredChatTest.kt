package cn.xindong.core

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Protocol fixtures, never real AI or quality scores. */
class StructuredChatTest {
    private val good="""{"facts":[],"hypotheses":[],"unknowns":["SYNTHETIC_UNKNOWN"],"strategy":"SYNTHETIC_STRATEGY","risks":[],"disagreements":[]}"""
    private val model=ModelConfig(id="fixture",name="fixture",endpoint="https://fixture.invalid/chat",model="fixture",apiKey="fixture",
        contextTokens=200_000,inputRmbPerMillion="2",outputRmbPerMillion="8",pricingVerified=true,pricingSource="unit fixture only")
    private fun invoke(bodies: List<String>): Pair<ValidatedReply<JSONObject>,Ledger> {
        val ledger=Ledger(); var calls=0
        val gateway=object: Gateway {
            override fun chat(config: ModelConfig,system: String,user: String,cancel: CancelToken,progress: (String)->Unit): ModelReply {
                assertTrue(calls<bodies.size); return ModelReply(bodies[calls++],"SYNTHETIC_FIXTURE",Usage(10,20))
            }
            override fun jev(config: ModelConfig,state: JSONObject,cancel: CancelToken): ModelReply = error("Jev disabled")
        }
        return StructuredChat.run(ChargedGateway(gateway,ledger,"fixture"),"狗头军师",model,"SYNTHETIC_CONTRACT","{\"history\":[]}",CancelToken()) {
            SkillOutput.parse("狗头军师",it,setOf("m1")) } to ledger
    }
    @Test fun validFirstResponseDoesNotPayForRepair() {
        val (r,l)=invoke(listOf(good)); assertTrue(r.repairs.isEmpty()); assertEquals(1,l.entries().size)
    }
    @Test fun malformedResponseCanBeReplacedOnlyByOneRealGatewayResultWithBothChargesAndHashes() {
        val bad=good.replace("\"strategy\":\"SYNTHETIC_STRATEGY\",","\"strategy\":\"SYNTHETIC_STRATEGY\"],")
        val (r,l)=invoke(listOf(bad,good)); assertEquals(2,l.entries().size)
        assertTrue(l.entries().all { it.state=="TOKEN_ESTIMATE" }); assertEquals(sha256(bad.toByteArray()),r.repairs.single().getString("initial_sha256"))
        assertEquals(sha256(good.toByteArray()),r.repairs.single().getString("repaired_sha256")); assertEquals(1,r.repairs.single().getInt("attempt"))
        assertFalse(r.repairs.single().toString().contains("SYNTHETIC_STRATEGY"))
    }
    @Test fun secondFailureCannotTriggerThirdPaidAttempt() {
        try { invoke(listOf("not-json","still-not-json")); fail("must reject") }
        catch(e: SkillOutputFailure) { assertEquals("INVALID_JSON",e.code) }
    }
    @Test fun foreignCitationNeverTriggersFormatRepair() {
        val bad=JSONObject(good).put("facts",arr(listOf(JSONObject().put("text","SYNTHETIC").put("evidence_ids",arr(listOf("foreign")))))).toString()
        try { invoke(listOf(bad)); fail("foreign scope must reject") }
        catch(e: SkillOutputFailure) { assertEquals("FOREIGN_REFERENCE",e.code) }
        assertFalse(StructuredChat.repairable(IllegalArgumentException("最近消息含明确停止联系要求；AI结果违反边界，已拒绝采用")))
    }
    @Test fun repairMayKeepUnknownButCannotInventMissingCitation() {
        val missing=JSONObject(good).put("facts",arr(listOf(JSONObject().put("text","SYNTHETIC_UNSUPPORTED").put("evidence_ids",arr(emptyList<String>()))))).toString()
        val (r,l)=invoke(listOf(missing,good)); assertEquals(0,r.value.getJSONArray("facts").length()); assertEquals(2,l.entries().size)
        try { invoke(listOf(missing,missing)); fail("still missing evidence") } catch(e: SkillOutputFailure) { assertEquals("SCHEMA",e.code) }
    }
}
