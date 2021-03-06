package sets

import gymlog.Application
import gymlog.controllers.SetsController
import gymlog.models.Sets.SetRow
import gymlog.models.Sets.Sets
import gymlog.services.SetsService.SETS_TABLE
import gymlog.services.SetsService.SET_ID_COLUMN
import gymlog.services.SetsService.USER_ID_COLUMN
import gymlog.utils.DatabaseUtils
import gymlog.utils.JsonUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import utils.InvokeActions.invokeAuthentication
import utils.InvokeActions.invokeDeleteWithAuth
import utils.InvokeActions.invokeGetWithAuth
import utils.InvokeActions.invokePostWithAuth
import utils.TestDbUtils
import java.math.BigDecimal
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@TestPropertySource(locations = ["classpath:junit.properties"])
class SetsTest {

    @Autowired
    val setsController: SetsController? = null

    @Autowired
    private val mvc: MockMvc? = null

    @Autowired
    @Qualifier("gymlogdatasource")
    private val gymlogDataSource: DataSource? = null

    private val log = LoggerFactory.getLogger(SetsTest::class.java)

    @Before
    fun init() {
        TestDbUtils.createSchema(gymlogDataSource!!, "gymlog_db")
        TestDbUtils.executeSqlFile(gymlogDataSource, "create-sets-tables.sql")
        TestDbUtils.executeSqlFile(gymlogDataSource, "create-auth-tables.sql")
    }

    @Test
    @Throws(Exception::class)
    fun contextLoads() {
        Assert.assertTrue(setsController != null)
        Assert.assertTrue(gymlogDataSource != null)
        Assert.assertTrue(mvc != null);
    }

    @Test
    fun testHeartbeat() {
        val result = invokeGetWithAuth(mvc!!, "/api/heartbeat", token = getToken("user", "pass")).andExpect(status().isOk).andReturn()
        val response = JsonUtils.jsonToObject(result.response.contentAsString, HashMap::class.java)
        Assert.assertEquals(200, result.response.status)
        Assert.assertEquals("ok", response["status"] as String)
    }

    @Test
    fun testGetSets() {
        val result = invokeGetWithAuth(mvc!!, "/api/sets", emptyMap(), token = getToken("user", "pass")).andExpect(status().isOk).andReturn()
        val response = JsonUtils.jsonToObject(result.response.contentAsString, Sets::class.java)
        val row = response.sets.first()
        Assert.assertEquals(1, response.total)
        Assert.assertEquals(200, result.response.status)
        Assert.assertEquals("set id 1", row.id)
        Assert.assertEquals("user", row.userId)
        Assert.assertEquals(10, row.repetitions)
        Assert.assertEquals("Squat", row.exercise)
        Assert.assertEquals(BigDecimal(102.5), row.weight)
    }

    @Test
    fun testGetSetsFailure() {
        invokeGetWithAuth(mvc!!, "/api/sets/foo", emptyMap(), token = getToken("user", "pass")).andExpect(status().isMethodNotAllowed).andReturn()
    }

    @Test
    fun testGetSetsWithoutResults() {
        val result = invokeGetWithAuth(mvc!!, "/api/sets", mapOf("userId" to "notfound"), token = getToken("notfound", "pass")).andExpect(status().isOk).andReturn()
        val response = JsonUtils.jsonToObject(result.response.contentAsString, Sets::class.java)
        Assert.assertEquals(0, response.sets.size)
    }

    @Test
    fun testDeleteSet() {
        val result = invokeDeleteWithAuth(mvc!!, "/api/sets/{setId}", pathVariables = arrayListOf("set id 1"), token = getToken("user", "pass")).andExpect(status().isOk).andReturn()
        val responseSet = JsonUtils.jsonToObject(result.response.contentAsString, SetRow::class.java)
        Assert.assertEquals("set id 1", responseSet.id)
        val foundRows = DatabaseUtils.doQuery(gymlogDataSource!!, "select * from $SETS_TABLE where $USER_ID_COLUMN = ? and $SET_ID_COLUMN = ?", mapOf(1 to "user", 2 to "set id 1"))
        Assert.assertEquals(0, foundRows.size)
    }

    @Test
    fun testDeleteSetFailure() {
        val result = invokeDeleteWithAuth(mvc!!, "/api/sets/{setId}", pathVariables = arrayListOf("notfound"), token = getToken("user", "pass")).andExpect(status().isNoContent).andReturn()
        val foundRows = DatabaseUtils.doQuery(gymlogDataSource!!, "select * from $SETS_TABLE", emptyMap())
        Assert.assertEquals(204, result.response.status)
        Assert.assertEquals(1, foundRows.size)
    }

    @Test
    fun testPostSet() {
        val body = JsonUtils.objectToJson(SetRow(null, "user", BigDecimal(105.0), "Deadlift", 15, Date(System.currentTimeMillis())))
        val result = invokePostWithAuth(mvc!!, "/api/sets", body = body, pathVariables = ArrayList(), token = getToken("user", "pass")).andExpect(status().isOk).andReturn()
        val responseSet = JsonUtils.jsonToObject(result.response.contentAsString, SetRow::class.java)
        Assert.assertNotNull(responseSet.id)
        Assert.assertEquals("user", responseSet.userId)
        Assert.assertEquals(BigDecimal(105.0), responseSet.weight)
        Assert.assertEquals("Deadlift", responseSet.exercise)
        Assert.assertEquals(15, responseSet.repetitions)
        Assert.assertNotNull(responseSet.createdDate)

        val foundRows = DatabaseUtils.doQuery(gymlogDataSource!!, "select * from $SETS_TABLE where $USER_ID_COLUMN = ?", mapOf(1 to "user"))
        Assert.assertEquals(2, foundRows.size)
    }

    @Test
    fun testPostSetFailure() {
        val body = JsonUtils.objectToJson(SetRow(null, "user", null, "Deadlift", 15, Date(System.currentTimeMillis())))
        invokePostWithAuth(mvc!!, "/api/sets", body = body, pathVariables = ArrayList(), token = getToken("user", "pass")).andExpect(status().isBadRequest).andReturn()
        val foundRows = DatabaseUtils.doQuery(gymlogDataSource!!, "select * from $SETS_TABLE;", emptyMap())
        Assert.assertEquals(1, foundRows.size)
    }

    private fun getToken(user: String, password: String): String {
        val body = JsonUtils.objectToJson(mapOf("username" to user, "password" to password))
        val tokenResult = invokeAuthentication(mvc!!, "/login", body = body).andExpect(status().isOk).andReturn()
        return (tokenResult.response.getHeaderValue("Authorization") as String)
    }
}